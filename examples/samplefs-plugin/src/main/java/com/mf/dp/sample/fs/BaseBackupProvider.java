package com.mf.dp.sample.fs;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.mf.dp.uic.util.DPUtil;
import com.mf.dp.uic.util.ExceptionUtil;

import com.mf.dp.sample.fs.model.BackupContext;
import com.mf.dp.sample.fs.model.SampleFSObjectOptions;
import com.mf.dp.sample.fs.model.SampleFSObjectVerOptions;
import com.mf.dp.sample.fs.model.RestoreContext;
import com.mf.dp.sample.fs.model.SampleFSBackupRequest;
import com.mf.dp.sample.fs.model.SampleFSLastBackupDetail;
import com.mf.dp.sample.fs.model.SampleFSRestoreRequest;
import com.mf.dp.sample.fs.service.DPService;

public abstract class BaseBackupProvider extends BackupProvider implements BackupInterface, RestoreInterface {
	
	protected Logger logger = LoggerFactory.getLogger(getClass()); 

	@Autowired
	protected Validator validator;

	@Autowired
	protected DPService dpService;

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
	
	protected void fullBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
		logger.info("Starting full backup...");
		status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_START_FULL_BACKUP));

		// Plugin-specific part of the full backup logic
		doFullBackup(status, request, context);
		
		// Transfer the backed-up data to DP MA
		sendFullBackup(status, request, context);
	}
	
	protected abstract void doFullBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context);

	protected void incrBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
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

	protected abstract void doIncrBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context, SampleFSLastBackupDetail lastBackupDetail);
	
	protected SampleFSLastBackupDetail getLastBackupDetail(IProgressStatus status, SampleFSBackupRequest request) {
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
	
	protected void checkIncrBackupConstraint(IProgressStatus status, SampleFSLastBackupDetail lastBackupDetail) {
		if(lastBackupDetail.getObjectVerOptions() == null) {
			logger.warn("Incremental backup requires previous backup in the chain");
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, NLSMessageTemplate.NLS_INCREMENTAL_REQUIRES_PREVIOUS_BACKUP));
			throw new ServiceException(HttpStatus.CONFLICT, "Incremental backup requires previous backup in the chain");
		}
		//TODO If you have additional validation to perform, add them directly here or override this method in SampleFSBackupProvider whichever is preferred.
	}

	protected BackupContext establishBackupContext(IProgressStatus status, SampleFSBackupRequest request) {
		try {
			return getBackupContext(request);
		} catch (Exception e) {
			logger.error("Could not prepare for backup operation", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_PREPARE_FOR_BACKUP, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}
	
	protected BackupContext getBackupContext(SampleFSBackupRequest request) {
		BackupContext context = new BackupContext();
		
		/// [Step 1] Set up staging areas to be used for backup. Two separate areas are needed:
		///		a) "data" directory - used for full backup data (required only for full backup)
		///		b) "log" directory - used for incr backup data (required for both full and incr, empty for full backup)

		// If full backup, set up a data directory in proper place required for backup.
		if(Constant.BACKUP_TYPE_FULL.equalsIgnoreCase(request.getBackupType())) {
			//TODO Specify data directory path which is where the full backed-up
			// data will be staged for transfer to Media Agent later on.
			// By default, the following code designates a temp directory as the
			// staging area for full backup. 
			// Depending on the characteristics of the application exposed through
			// the plugin, non-staged backup may be feasible or even required.
			// In such case, specify the custom location. For example, if the 
			// location of full backup is specified as an input value in the backup
			// request, copy the value into the context, as shown here.
			//context.setDataDirPath(request.getAppOptions().getLocationOfFullBackup());
			
			String dataDirPath = createBackupTempDirectory("data-" + context.getTime());
			context.setDataDirPath(dataDirPath);
			context.setDeleteDataDirAfterUse(true);
		}
		
		// Set up a log directory in proper place required for backup.
		// The log directory must be empty to begin with, and should be deleted
		// after use in most cases.
		// The log directory is required for both full and incr backups, although,
		// in the case of full backup, the log directory will be empty.
		String logDirPath = createBackupTempDirectory("log-" + context.getTime());
		context.setLogDirPath(logDirPath);
		context.setDeleteLogDirAfterUse(true);
		
		
		/// [Step 2] Prepare "object options" data structure required by DP
		
		SampleFSObjectOptions objectOptions = new SampleFSObjectOptions();
		context.setObjectOptions(objectOptions);
		objectOptions.setAppName(request.getAppOptions().getAppName());
		objectOptions.setAppId(request.getAppOptions().getAppId());
		//TODO If the plugin needs to store additional information in the object options,
		// add them here.
		
		/// [Step 3] Prepare "object version options" data structure required by DP
		
		SampleFSObjectVerOptions objectVerOptions = new SampleFSObjectVerOptions();
		context.setObjectVerOptions(objectVerOptions);
		objectVerOptions.setSourceClient(request.getAppHost());
		//TODO If the plugin needs to store additional information in the object version 
		// options, add them here. If the additional information cannot be obtained until
		// after the backup is complete (e.g. size of backup, etc.), add them in the
		// doFullBackup() method overriden by the plugin.
		
		/// [Step 4] Prepare plugin-specific fields in the context
		//TODO If the plugin defines additional fields in the context object, set them up here.
		
		/// NOTE: You can add custom behaviors directly in this method or override this method in SampleFSBackupProvider whichever is preferred.
		
		return context;
	}
	
	protected String createBackupTempDirectory(String subDirName) {
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
	
	protected void cleanup(IProgressStatus status, BackupContext context) {
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
	
	protected void sendFullBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
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
	
	protected void sendIncrBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
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
	
	protected SampleFSBackupRequest validateInput(IProgressStatus status, BackupRequest backupRequest) {
		try {
			// Validate the JSON parsing (deserialization) of the SampleFS specific part of the request
			SampleFSBackupRequest request = toSampleFSBackupRequest(backupRequest);
			// Basic validation of the input fields
			request.validate(validator);
			//TODO If the plugin needs to perform additional (typically, non-syntactic) validation
			// on the request data, add them here. Alternatively, you could override this method
			// in SampleFSBackupProvider whichever is preferred.
			return request;
		} catch (Exception e) {
			logger.error("Input validation failed", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_INPUT_VALIDATION_FAILED, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}

	protected SampleFSBackupRequest toSampleFSBackupRequest(BackupRequest backupRequest) {
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
	
	protected void receiveBackup(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext restoreContext) {
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

	protected abstract void doRestoreFullBackup(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext restoreContext);

	protected void applyIncrementalBackupInSequence(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext restoreContext) {
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
	
	protected abstract void doRestoreIncrBackup(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext restoreContext, File incrBackupDir);

	protected SampleFSRestoreRequest validateInput(IProgressStatus status, RestoreRequest restoreRequest) {
		try {
			// Validate the JSON parsing (deserialization) of the SampleFS specific part of the request
			SampleFSRestoreRequest request = toSampleFSRestoreRequest(restoreRequest);
			// Basic validation of the input fields
			request.validate(validator);
			//TODO If the plugin needs to perform additional (typically, non-syntactic) validation
			// on the request data, add them here. Alternatively, you could override this method
			// in SampleFSBackupProvider whichever is preferred.
			return request;
		} catch (Exception e) {
			logger.error("Input validation failed", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_INPUT_VALIDATION_FAILED, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}

	protected SampleFSRestoreRequest toSampleFSRestoreRequest(RestoreRequest restoreRequest) {
		try {
			return new SampleFSRestoreRequest(restoreRequest);
		} catch (JsonProcessingException e) {
			throw new ServiceException(HttpStatus.BAD_REQUEST);
		}
	}

	protected RestoreContext establishRestoreContext(IProgressStatus status, SampleFSRestoreRequest request) {
		try {
			return getRestoreContext(request);
		} catch (Exception e) {
			logger.error("Could not prepare for restore operation", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_PREPARE_FOR_RESTORE, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}
	
	protected RestoreContext getRestoreContext(SampleFSRestoreRequest request) {
		RestoreContext context = new RestoreContext();
		
		/// [Step 1] Set up staging areas to be used for restore. Two separate areas are needed:
		///		a) "data" directory - used for a single full backup data
		///		b) "log" directory - used for zero or more incremental backup data

		//TODO Specify data directory path which is where the full backed-up
		// data will be transfered to from the Media Agent later on.
		// By default, the following code designates a temp directory as the
		// staging area for receiving full backup. In such case, the plugin
		// will later apply the received full backup to the application during
		// restore.		
		// Depending on the characteristics of the application exposed through
		// the plugin, non-staged restore of full backup may be feasible or
		// even requird. In such case, specify the custom location. For example,
		// it is not uncommon for user to specify the location of the restore 
		// as an input value in the restore request. If so, copy the value from
		// the request into the context, as shown here.
		//context.setDataDirPath(request.getAppOptions().getLocationOfFullBackupRestore());
		
		String dataDirPath = createRestoreTempDirectory("data-" + context.getTime());
		context.setDataDirPath(dataDirPath);	
		context.setDeleteDataDirAfterUse(true);

		// Set up a log directory in proper place required for restore.
		// The log directory must be empty to begin with, and should be deleted
		// after use in most cases.
		String logDirPath = createRestoreTempDirectory("log-" + context.getTime());
		context.setLogDirPath(logDirPath);
		context.setDeleteLogDirAfterUse(true);

		/// [Step 2] Prepare plugin-specific fields in the context
		//TODO If the plugin defines additional fields in the context object, set them up here.

		/// NOTE: You can add custom behaviors directly in this method or override this method in SampleFSBackupProvider whichever is preferred.
		
		return context;
	}

	protected String createRestoreTempDirectory(String subDirName) {
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
	
	protected void cleanup(IProgressStatus status, RestoreContext context) {
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

}
