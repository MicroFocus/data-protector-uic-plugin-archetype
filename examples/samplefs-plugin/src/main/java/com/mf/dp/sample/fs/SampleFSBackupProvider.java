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

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.mf.dp.sample.fs.model.BackupContext;
import com.mf.dp.sample.fs.model.RestoreContext;
import com.mf.dp.sample.fs.model.SampleFSBackupRequest;
import com.mf.dp.sample.fs.model.SampleFSLastBackupDetail;
import com.mf.dp.sample.fs.model.SampleFSObjectOptions;
import com.mf.dp.sample.fs.model.SampleFSObjectVerOptions;
import com.mf.dp.sample.fs.model.SampleFSRestoreRequest;
import com.mf.dp.sample.fs.service.FSService;
import com.mf.dp.uic.exception.ServiceException;
import com.mf.dp.uic.model.BackupRequest;
import com.mf.dp.uic.model.RestoreRequest;
import com.mf.dp.uic.model.SessionReport;
import com.mf.dp.uic.model.SessionReport.MessageType;
import com.mf.dp.uic.plugin.spi.IProgressStatus;
import com.mf.dp.uic.util.ConfigProperties;
import com.mf.dp.uic.util.ExceptionUtil;

@Component
@PropertySources({
    @PropertySource("classpath:samplefs.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="classpath:config/samplefs.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:./samplefs.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:./config/samplefs.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:${dpuic.config.dirpath}/samplefs.properties"),
})
public class SampleFSBackupProvider extends BaseBackupProvider {
	
	@Autowired
	private FSService fsService;

	@Override
	protected void checkIncrBackupConstraint(IProgressStatus status, SampleFSLastBackupDetail lastBackupDetail) {
		super.checkIncrBackupConstraint(status, lastBackupDetail);
		if(lastBackupDetail.getObjectVerOptions().getBackupTime() <= 0) {
			logger.error("Encountered invalid backup time of {}", lastBackupDetail.getObjectVerOptions().getBackupTime());
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_UNEXPECTED_ERROR, "Encountered invalid backup time")));
			throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Report to administrator: ");
		}
	}

	@Override
	protected BackupContext getBackupContext(SampleFSBackupRequest request) {
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
		
		// Set up object version options
		SampleFSObjectVerOptions objectVerOptions = new SampleFSObjectVerOptions();
		context.setObjectVerOptions(objectVerOptions);
		objectVerOptions.setSourceClient(request.getAppHost());
		objectVerOptions.setBackupType(request.getBackupType());
		objectVerOptions.setBackupTime(context.getTime());
				
		return context;
	}

	@Override
	protected SampleFSBackupRequest validateInput(IProgressStatus status, BackupRequest backupRequest) {
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
	
	@Override
	protected SampleFSRestoreRequest validateInput(IProgressStatus status, RestoreRequest restoreRequest) {
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

	@Override
	protected RestoreContext getRestoreContext(SampleFSRestoreRequest request) {
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
	
	@Override
	protected void doFullBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context) {
		// For SampleFS, there is no separate step for creating full backup data and
		// staging it in a known location. Instead, the context.dataDirPath field was
		// set to the location of the source directory earlier in the getBackupContext()
		// method. Consequently, the data will be transfered directly from the source
		// directly to the Media Agent after this method returns.
		// No processing required here.
	}
	
	@Override
	protected void doIncrBackup(IProgressStatus status, SampleFSBackupRequest request, BackupContext context, SampleFSLastBackupDetail lastBackupDetail) {
		// Copy all files modified since the last backup time to the prepared log directory
		fsService.copyFilesModifiedSince(request.getAppOptions().getDirPath(), context.getLogDirPath(),
				lastBackupDetail.getObjectVerOptions().getBackupTime(),
				ConfigProperties.getPropertyBoolean("samplefs.backup.follow-symbolic-links"));
	}

	@Override
	protected void doRestoreFullBackup(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext context) {
		sendEmailNotification(request);

		// No additional processing is needed for the full backup, because
		// the backed-up data was copied directly into the target directory
		// (i.e., no intermediate staging area).
	}
	
	@Override
	protected void doRestoreIncrBackup(IProgressStatus status, SampleFSRestoreRequest request, RestoreContext context, File incrBackupDir) {
		fsService.copyContent(incrBackupDir.getAbsolutePath(), request.getAppOptions().getRestoreDirPath());
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
