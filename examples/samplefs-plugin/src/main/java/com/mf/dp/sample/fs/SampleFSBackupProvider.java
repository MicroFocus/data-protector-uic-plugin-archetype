/*
 * Copyright (c) 2023 Micro Focus or one of its affiliates.
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mf.dp.sample.fs;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mf.dp.sample.fs.model.BackupContext;
import com.mf.dp.sample.fs.model.RestoreContext;
import com.mf.dp.sample.fs.model.SampleFSBackupRequest;
import com.mf.dp.sample.fs.model.SampleFSLastBackupDetail;
import com.mf.dp.sample.fs.model.SampleFSObjectOptions;
import com.mf.dp.sample.fs.model.SampleFSObjectVerOptions;
import com.mf.dp.sample.fs.model.SampleFSRestoreRequest;
import com.mf.dp.sample.fs.service.DPService;
import com.mf.dp.sample.fs.service.FSService;
import com.mf.dp.uic.command.OSCommand;
import com.mf.dp.uic.exception.ServiceException;
import com.mf.dp.uic.model.BackupRequest;
import com.mf.dp.uic.model.RestoreRequest;
import com.mf.dp.uic.model.SessionReport;
import com.mf.dp.uic.model.SessionReport.MessageType;
import com.mf.dp.uic.plugin.spi.BackupInterface;
import com.mf.dp.uic.plugin.spi.BackupProvider;
import com.mf.dp.uic.plugin.spi.IProgressStatus;
import com.mf.dp.uic.plugin.spi.RestoreInterface;
import com.mf.dp.uic.util.ConfigProperties;
import com.mf.dp.uic.util.DPUtil;
import com.mf.dp.uic.util.ExceptionUtil;

@Component
@PropertySources({
    @PropertySource("classpath:samplefs.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="classpath:config/samplefs.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:./samplefs.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:./config/samplefs.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:${dpuic.config.dirpath}/samplefs.properties"),
})
public class SampleFSBackupProvider extends BackupProvider implements BackupInterface, RestoreInterface {

	private Logger logger = LoggerFactory.getLogger(getClass()); 

	@Autowired
	private Validator validator;

	@Autowired
	private DPService dpService;

	@Autowired
	private FSService fsService;
	
	@Override
	public void backup(IProgressStatus status, BackupRequest backupRequest)
			throws ServiceException, InterruptedException {
		// Transform and validate the input
		SampleFSBackupRequest request = validateInput(status, backupRequest);
		
		// Establish backup context (convenience object to work with during backup)	
		BackupContext backupContext = establishBackupContext(status, request);

		try {
			// Perform backup
			if(Constant.BACKUP_TYPE_FULL.equalsIgnoreCase(backupRequest.getBackupType())) {
				fullBackup(status, request, backupContext);
			} else {
				incrBackup(status, request, backupContext);
			}
		} finally {
			// Clean up resources
			cleanup(status, backupContext);
		}	
	}
	
	private void fullBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
		logger.info("Starting full backup...");
		status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_START_FULL_BACKUP));

		// Plugin-specific part of the full backup logic
		doFullBackup(status, request, context);
		
		// Transfer the backed-up data to DP MA
		sendFullBackup(status, request, context);
	}
	
	private void doFullBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
		// For SampleFS, there is no separate step for creating full backup data and
		// staging it in a known location. Instead, the context.dataDirPath field was
		// set to the location of the source directory earlier in the getBackupContext()
		// method. Consequently, the data will be transfered directly from the source
		// directly to the Media Agent after this method returns.
		// No processing required here.
	}

	private void incrBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
		logger.info("Starting incremental backup...");
		status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_START_INCREMENTAL_BACKUP));

		// Obtain last backup details from DP IDB
		SampleFSLastBackupDetail lastBackupDetail = getLastBackupDetail(status, request);
		
		// Check incr backup constraint
		checkIncrBackupConstraint(status, lastBackupDetail);
		
		// Plugin-specific part of the incr backup logic
		doIncrBackup(status, request, context, lastBackupDetail);
		
		// Transfer the data to DP MA
		sendIncrBackup(status, request, context);
	}

	private void doIncrBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context, SampleFSLastBackupDetail lastBackupDetail) {
		// Copy all files modified since the last backup time to the prepared log directory
		fsService.copyFilesModifiedSince(request.getAppOptions().getDirPath(), context.getLogDirPath(),
				lastBackupDetail.getObjectVerOptions().getBackupTime(),
				ConfigProperties.getPropertyBoolean("samplefs.backup.follow-symbolic-links"));
	}
	
	private SampleFSLastBackupDetail getLastBackupDetail(IProgressStatus status, SampleFSBackupRequest request) {
		logger.info("Retrieving last backup details from Session Manager");
		status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_GET_LAST_BACKUP_DETAILS));

		try {
			return dpService.getLastBackupDetail(request);
		} catch (Exception e) {
			logger.error("Could not obtain last backup details", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_OBTAIN_LAST_BACKUP_DETAIL, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}
	
	private void checkIncrBackupConstraint(IProgressStatus status, SampleFSLastBackupDetail lastBackupDetail) {
		if(lastBackupDetail.getObjectVerOptions() == null) {
			logger.warn("Incremental backup requires previous backup in the chain");
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, NLSMessageTemplate.NLS_INCREMENTAL_REQUIRES_PREVIOUS_BACKUP));
			throw new ServiceException(HttpStatus.CONFLICT, "Incremental backup requires previous backup in the chain");
		}
		
		if(lastBackupDetail.getObjectVerOptions().getBackupTime() <= 0) {
			logger.error("Encountered invalid backup time of {}", lastBackupDetail.getObjectVerOptions().getBackupTime());
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_UNEXPECTED_ERROR, "Encountered invalid backup time")));
			throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Report to administrator: ");
		}
	}

	private BackupContext establishBackupContext(IProgressStatus status, SampleFSBackupRequest request) {
		try {
			return getBackupContext(request);
		} catch (Exception e) {
			logger.error("Could not prepare for backup operation", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_PREPARE_FOR_BACKUP, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}
	
	private BackupContext getBackupContext(SampleFSBackupRequest request) {
		BackupContext context = new BackupContext();
		
		// If full backup, set up a data directory in proper place required for backup.
		if(Constant.BACKUP_TYPE_FULL.equalsIgnoreCase(request.getBackupType())) {
			// With SampleFS, the data transfer will take place directly on the
			// source directory without a staging area.
			context.setDataDirPath(request.getAppOptions().getDirPath());
			// Do NOT call context.setDeleteDataDirAfterUse(true) in this case, 
			// since we don't want the data source to be deleted after backup!!
		}
		
		// Set up a log directory in proper place required for backup.		
		// The log directory must be empty to begin with and should be deleted after use.
		String logDirPath = createBackupTempDirectory("log-" + context.getTime());
		context.setLogDirPath(logDirPath);
		context.setDeleteLogDirAfterUse(true);
		
		// Set up object options
		SampleFSObjectOptions objectOptions = new SampleFSObjectOptions();
		context.setObjectOptions(objectOptions);
		objectOptions.setAppName(request.getAppOptions().getAppName());
		objectOptions.setAppId(request.getAppOptions().getAppId());
		objectOptions.setDirPaths(new String[] {request.getAppOptions().getDirPath()});
		
		// Set up object version options
		SampleFSObjectVerOptions objectVerOptions = new SampleFSObjectVerOptions();
		context.setObjectVerOptions(objectVerOptions);
		objectVerOptions.setSourceClient(request.getAppHost());
		objectVerOptions.setBackupType(request.getBackupType());
		objectVerOptions.setBackupTime(context.getTime());
				
		return context;
	}
	
	private String createBackupTempDirectory(String subDirName) {
		String dirPath = DPUtil.getBackupTempDirectory(Constant.PLUGIN_NAME, subDirName);
		
		try {
			OSCommand osCommand = new OSCommand();
			// Create the backup directory as needed
			osCommand.createDirectories(dirPath);
			// Make sure that the backup directory is clean/empty
			osCommand.removeDirectoryContent(dirPath);
			// Set proper access for security
			osCommand.chmod(dirPath, 600, false);			
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}

		return dirPath;
	}
	
	private void cleanup(IProgressStatus status, BackupContext context) {
		// Clean up the data directory if the system owns it (i.e. a temp directory)
		if(context.isDeleteDataDirAfterUse()) {
			String dataDirPath = context.getDataDirPath();
			try {
				new OSCommand().removeDirectoryAndContent(dataDirPath);
				logger.info("Removed the backup data directory {} from the local system", dataDirPath);
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, String.format(NLSMessageTemplate.NLS_DATA_DIR_REMOVED, dataDirPath)));
			} catch (Exception e) {
				logger.error("Could not remove the backup data directory {}", dataDirPath, e);
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_REMOVE_DATA_DIR, dataDirPath, ExceptionUtil.getMessageForSessionReport(e))));
				throw ExceptionUtil.convertToServiceException(e);
			}
		}

		// Clean up the log directory if the system owns it (i.e. a temp directory)
		if(context.isDeleteLogDirAfterUse()) {
			String logDirPath = context.getLogDirPath();
			try {
				new OSCommand().removeDirectoryAndContent(logDirPath);
				logger.info("Removed the backup log directory {} from the local system", logDirPath);
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, String.format(NLSMessageTemplate.NLS_LOG_DIR_REMOVED, logDirPath)));
			} catch (Exception e) {
				logger.error("Could not remove the backup log directory {}", logDirPath, e);
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_REMOVE_LOG_DIR, logDirPath, ExceptionUtil.getMessageForSessionReport(e))));
				throw ExceptionUtil.convertToServiceException(e);
			}
		}
	}
	
	private void sendFullBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
		logger.info("Transferring full backup data to MA. This may take a while...");
		status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_TRANSFER_TO_MA));

		try {
			dpService.sendFullBackup(status, request, context);
			logger.info("Transfer of full backup data to MA is complete");
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_TRANSFER_TO_MA_COMPLETED));
		} catch (Exception e) {
			logger.error("Could not transfer full backup data to MA", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_TRANSFER_TO_MA_FAILED, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}	
	}
	
	private void sendIncrBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
		logger.info("Transferring incremental backup data to MA. This may take a while...");
		status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_TRANSFER_TO_MA));

		try {
			dpService.sendIncrBackup(status, request, context);
			logger.info("Transfer of incremental backup data to MA is complete");
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_TRANSFER_TO_MA_COMPLETED));
		} catch (Exception e) {
			logger.error("Could not transfer incremental backup data to MA", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_TRANSFER_TO_MA_FAILED, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}	
	}
	
	private SampleFSBackupRequest validateInput(IProgressStatus status, BackupRequest backupRequest) {
		try {
			// Validate the JSON parsing (deserialization) of the SampleFS specific part of the request
			SampleFSBackupRequest request = toSampleFSBackupRequest(backupRequest);
			// Basic validation of the input fields
			request.validate(validator);
			// Make sure that the specified directory to back up is indeed a directory
			fsService.assertDirectory(request.getAppOptions().getDirPath(), true, false);
			return request;
		} catch (Exception e) {
			logger.error("Input validation failed", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_INPUT_VALIDATION_FAILED, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}

	private SampleFSBackupRequest toSampleFSBackupRequest(BackupRequest backupRequest) {
		try {
			return new SampleFSBackupRequest(backupRequest);
		} catch (JsonProcessingException e) {
			throw new ServiceException(HttpStatus.BAD_REQUEST);
		}
	}

	@Override
	public void restore(IProgressStatus status, RestoreRequest restoreRequest)
			throws ServiceException, InterruptedException {
		// Transform and validate the input
		SampleFSRestoreRequest request = validateInput(status, restoreRequest);

		// Establish restore context (convenience object to work with during backup)	
		RestoreContext restoreContext = establishRestoreContext(status, request);

		try {		
			logger.info("Starting restore...");
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_START_RESTORE));

			// Retrieve the chain of backed up data
			receiveBackup(status, request, restoreContext);
			
			// First, apply the single full backup
			doRestoreFullBackup(status, request, restoreContext);

			// Second, apply the sequence of zero or more incremental backups
			applyIncrementalBackupInSequence(status, request, restoreContext);
		} finally {
			// Clean up resources
			cleanup(status, restoreContext);
		}
	}
	
	private void receiveBackup(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext restoreContext) {
		logger.info("Transferring backup data from MA. This may take a while...");
		status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_TRANSFER_FROM_MA));

		try {
			dpService.receiveBackupFromDP(status, request, restoreContext);
			logger.info("Transfer of backup data from MA is complete");
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_TRANSFER_FROM_MA_COMPLETED));
		} catch (Exception e) {
			logger.error("Could not transfer backup data from MA", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_TRANSFER_FROM_MA_FAILED, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}

	private void doRestoreFullBackup(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext context) {
		sendEmailNotification(request);

		// No additional processing is needed for the full backup, because
		// the backed-up data was copied directly into the target directory
		// (i.e., no intermediate staging area).
	}

	private void applyIncrementalBackupInSequence(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext restoreContext) {
		// Get the sorted list of incremental backups to apply, if any	
		List<File> incrBackups;
		try {
			incrBackups = Arrays.stream(new File(restoreContext.getLogDirPath()).listFiles())
					.filter(file -> !file.getName().equals("0")) // Exclude LOG:0 of full backup
					.sorted(Comparator.comparing(file -> Integer.parseInt(file.getName())))
					.collect(Collectors.toList());
		} catch(Exception e) {
			logger.error("Failed to get sorted list of incremental backups", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_UNEXPECTED_ERROR, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
				
		// Apply the incremental backups in order
		if(incrBackups.isEmpty()) {
			logger.info("There is no incremental backup to apply");
		} else {
			logger.info("There are {} incremental backups to apply", incrBackups.size());
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, String.format(NLSMessageTemplate.NLS_APPLYING_INCREMENTAL, incrBackups.size())));
			try {
				for(int i = 0; i < incrBackups.size(); i++) {
					logger.info("({}) Applying the content of {}", (i+1), incrBackups.get(i).getAbsoluteFile());
					// Apply the single incremental backup
					doRestoreIncrBackup(status, request, restoreContext, incrBackups.get(i));
				}
				logger.info("All incremental backups have been applied", String.valueOf(incrBackups.size()));
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_APPLYING_INCREMENTAL_COMPLETED));
			} catch(Exception e) {
				logger.error("Could not apply incremental backups", e);
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_APPLY_INCREMENTAL, ExceptionUtil.getMessageForSessionReport(e))));
				throw e;
			}
		}
	}
	
	private void doRestoreIncrBackup(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext context, File incrBackupDir) {
		fsService.copyContent(incrBackupDir.getAbsolutePath(), request.getAppOptions().getRestoreDirPath());
	}

	private SampleFSRestoreRequest validateInput(IProgressStatus status, RestoreRequest restoreRequest) {
		try {
			// Validate the JSON parsing (deserialization) of the SampleFS specific part of the request
			SampleFSRestoreRequest request = toSampleFSRestoreRequest(restoreRequest);
			// Basic validation of the input fields
			request.validate(validator);
			// Make sure that the specified directory to restore the backed up data to is
			// indeed a directory AND empty.
			fsService.assertDirectory(request.getAppOptions().getRestoreDirPath(), true, true);
			return request;
		} catch (Exception e) {
			logger.error("Input validation failed", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_INPUT_VALIDATION_FAILED, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}

	private SampleFSRestoreRequest toSampleFSRestoreRequest(RestoreRequest restoreRequest) {
		try {
			return new SampleFSRestoreRequest(restoreRequest);
		} catch (JsonProcessingException e) {
			throw new ServiceException(HttpStatus.BAD_REQUEST);
		}
	}

	private RestoreContext establishRestoreContext(IProgressStatus status, SampleFSRestoreRequest request) {
		try {
			return getRestoreContext(request);
		} catch (Exception e) {
			logger.error("Could not prepare for restore operation", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_PREPARE_FOR_RESTORE, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}
	
	private RestoreContext getRestoreContext(SampleFSRestoreRequest request) {
		RestoreContext context = new RestoreContext();
		
		// Specify data directory path which is where the full backed-up
		// data will be transfered to from the Media Agent later on.
		// With SampleFS, the full backup data is transfered directly to the
		// target restore directory without a staging area.
		context.setDataDirPath(request.getAppOptions().getRestoreDirPath());
		// Do NOT call context.setDeleteDataDirAfterUse(true) in this case, since we 
		// don't want the restored target directory to be deleted right after restore!!
		
		// Set up a log directory in proper place required for restore.
		// The log directory must be empty to begin with and should be deleted after use.
		String logDirPath = createRestoreTempDirectory("log-" + context.getTime());
		context.setLogDirPath(logDirPath);
		context.setDeleteLogDirAfterUse(true);

		return context;
	}

	private String createRestoreTempDirectory(String subDirName) {
		String dirPath = DPUtil.getRestoreTempDirectory(Constant.PLUGIN_NAME, subDirName);
		
		try {
			OSCommand osCommand = new OSCommand();
			// Create the restore directory as needed
			osCommand.createDirectories(dirPath);
			// Make sure that the restore directory is clean/empty
			osCommand.removeDirectoryContent(dirPath);
		} catch (Exception e) {
			throw ExceptionUtil.convertToServiceException(e);
		}

		return dirPath;
	}
	
	private void cleanup(IProgressStatus status, RestoreContext context) {
		// Clean up the data directory if the system owns it (i.e. a temp directory)
		if(context.isDeleteDataDirAfterUse()) {
			String dataDirPath = context.getDataDirPath();
			try {
				new OSCommand().removeDirectoryAndContent(dataDirPath);
				logger.info("Removed the restore data directory {} from the local system", dataDirPath);
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, String.format(NLSMessageTemplate.NLS_DATA_DIR_REMOVED, dataDirPath)));
			} catch (Exception e) {
				logger.error("Could not remove the restore data directory {}", dataDirPath, e);
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_REMOVE_DATA_DIR, dataDirPath, ExceptionUtil.getMessageForSessionReport(e))));
				throw ExceptionUtil.convertToServiceException(e);
			}
		}

		// Clean up the log directory if the system owns it (i.e. a temp directory)
		if(context.isDeleteLogDirAfterUse()) {
			String logDirPath = context.getLogDirPath();
			try {
				new OSCommand().removeDirectoryAndContent(logDirPath);
				logger.info("Removed the restore log directory {} from the local system", logDirPath);
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, String.format(NLSMessageTemplate.NLS_LOG_DIR_REMOVED, logDirPath)));
			} catch (Exception e) {
				logger.error("Could not remove the restore log directory " + logDirPath, e);
				status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_REMOVE_LOG_DIR, logDirPath, ExceptionUtil.getMessageForSessionReport(e))));
				throw ExceptionUtil.convertToServiceException(e);
			}
		}
	}
	
	private void sendEmailNotification(SampleFSRestoreRequest request) {
		// NOTE: DP neither requires nor recommends sending an email notification
		//       out to administrator whenever a backup or restore starts.
		//       The SOLE purpose of this code is to illustrate:
		//       (1) Using ConfigProperties class to access values from the
		//       configuration file (samplefs.properties).
		//       (2) Using classes from 3rd party library/dependency that is
		//       packaged directly with the plugin's uber jar, because it is
		//       NOT included in the UIC's executable jar as its dependencies.

		if (ConfigProperties.getPropertyBoolean("samplefs.email.notification.enabled", false)) {
			Email email = new SimpleEmail();
			email.setHostName(ConfigProperties.getPropertyString("samplefs.email.hostname"));
			email.setSmtpPort(ConfigProperties.getPropertyInteger("samplefs.email.port", 465));
			email.setAuthenticator(new DefaultAuthenticator(ConfigProperties.getPropertyString("samplefs.email.auth.user"),
					ConfigProperties.getPropertyString("samplefs.email.auth.password")));
			email.setSSLOnConnect(true);
			email.setSSLCheckServerIdentity(true);
			try {
				email.setFrom(ConfigProperties.getPropertyString("samplefs.email.from"));
				email.setSubject("DP Restore Notification");
				email.setMsg("Restoration of full backup started...\n\tHost:" + request.getAppHost() + "\n\tSession ID:" + request.getSessionId());
				email.addTo(ConfigProperties.getPropertyString("samplefs.email.to"));
				email.send();
			} catch (EmailException e) {
				logger.warn("Failed to send notification email to administrator", e);
			}
		}
	}

}
