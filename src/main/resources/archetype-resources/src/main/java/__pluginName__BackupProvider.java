package ${package};

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
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

import ${package}.model.BackupContext;
import ${package}.model.${pluginName}ObjectOptions;
import ${package}.model.${pluginName}ObjectVerOptions;
import ${package}.model.RestoreContext;
import ${package}.model.${pluginName}BackupRequest;
import ${package}.model.${pluginName}LastBackupDetail;
import ${package}.model.${pluginName}RestoreRequest;
import ${package}.service.DPService;

@Component
@PropertySources({
    @PropertySource("classpath:${pluginNameLowerCase}.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="classpath:config/${pluginNameLowerCase}.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:./${pluginNameLowerCase}.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:./config/${pluginNameLowerCase}.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:${dpuic.config.dirpath}/${pluginNameLowerCase}.properties"),
})
public class ${pluginName}BackupProvider extends BackupProvider implements BackupInterface, RestoreInterface {
		
	private Logger logger = LoggerFactory.getLogger(getClass()); 

	@Autowired
	private Validator validator;

	@Autowired
	private DPService dpService;

	//TODO Autowire additional services you may have defined

	@Override
	public void backup(IProgressStatus status, BackupRequest backupRequest)
			throws ServiceException, InterruptedException {
		// Transform and validate the input
		${pluginName}BackupRequest request = validateInput(status, backupRequest);
		
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
	
	private void fullBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context) {
		logger.info("Starting full backup...");
		status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_START_FULL_BACKUP));

		// Plugin-specific part of the full backup logic
		doFullBackup(status, request, context);
		
		// Transfer the backed-up data to DP MA
		sendFullBackup(status, request, context);
	}
	
	private void doFullBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context) {
		//TODO Perform full backup using a technique or tool desirable for the application
		// for which this plugin is developed. Conceptually, it involves obtaining the latest
		// and consistent state of the application data, and then staging them in the prepared
		// data directory as designated by the context.dataDirPath field.
	}

	private void incrBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context) {
		logger.info("Starting incremental backup...");
		status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_NORMAL, NLSMessageTemplate.NLS_START_INCREMENTAL_BACKUP));

		// Obtain last backup details from DP IDB
		${pluginName}LastBackupDetail lastBackupDetail = getLastBackupDetail(status, request);
		
		// Check incr backup constraint
		checkIncrBackupConstraint(status, lastBackupDetail);
		
		// Plugin-specific part of the incr backup logic
		doIncrBackup(status, request, context, lastBackupDetail);
		
		// Transfer the data to DP MA
		sendIncrBackup(status, request, context);
	}

	private void doIncrBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context, ${pluginName}LastBackupDetail lastBackupDetail) {
		//TODO Perform incremental backup using a technique or tool desirable for the application
		// for which this plugin is developed. Conceptually, it involves obtaining the application
		// data delta that have changed since the last time it was backed up, and then staging
		// them in the prepared log directory as designated by the context.logDirPath field.
	}
	
	private ${pluginName}LastBackupDetail getLastBackupDetail(IProgressStatus status, ${pluginName}BackupRequest request) {
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
	
	private void checkIncrBackupConstraint(IProgressStatus status, ${pluginName}LastBackupDetail lastBackupDetail) {
		if(lastBackupDetail.getObjectVerOptions() == null) {
			logger.warn("Incremental backup requires previous backup in the chain");
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, NLSMessageTemplate.NLS_INCREMENTAL_REQUIRES_PREVIOUS_BACKUP));
			throw new ServiceException(HttpStatus.CONFLICT, "Incremental backup requires previous backup in the chain");
		}
		//TODO If you have additional validation to perform, add them here.
	}

	private BackupContext establishBackupContext(IProgressStatus status, ${pluginName}BackupRequest request) {
		try {
			return getBackupContext(request);
		} catch (Exception e) {
			logger.error("Could not prepare for backup operation", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_PREPARE_FOR_BACKUP, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}
	
	private BackupContext getBackupContext(${pluginName}BackupRequest request) {
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
		
		${pluginName}ObjectOptions objectOptions = new ${pluginName}ObjectOptions();
		context.setObjectOptions(objectOptions);
		objectOptions.setAppName(request.getAppOptions().getAppName());
		objectOptions.setAppId(request.getAppOptions().getAppId());
		//TODO If the plugin needs to store additional information in the object options,
		// add them here.
		
		/// [Step 3] Prepare "object version options" data structure required by DP
		
		${pluginName}ObjectVerOptions objectVerOptions = new ${pluginName}ObjectVerOptions();
		context.setObjectVerOptions(objectVerOptions);
		objectVerOptions.setSourceClient(request.getAppHost());
		//TODO If the plugin needs to store additional information in the object version 
		// options, add them here. If the additional information cannot be obtained until
		// after the backup is complete (e.g. size of backup, etc.), add them in the
		// doFullBackup() method overriden by the plugin.
		
		/// [Step 4] Prepare plugin-specific fields in the context
		//TODO If the plugin defines additional fields in the context object, set them up here.
		
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
	
	private void sendFullBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context) {
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
	
	private void sendIncrBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context) {
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
	
	private ${pluginName}BackupRequest validateInput(IProgressStatus status, BackupRequest backupRequest) {
		try {
			// Validate the JSON parsing (deserialization) of the ${pluginName} specific part of the request
			${pluginName}BackupRequest request = to${pluginName}BackupRequest(backupRequest);
			// Basic validation of the input fields
			request.validate(validator);
			//TODO If the plugin needs to perform additional (typically, non-syntactic) validation
			// on the request data, add them here.
			return request;
		} catch (Exception e) {
			logger.error("Input validation failed", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_INPUT_VALIDATION_FAILED, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}

	private ${pluginName}BackupRequest to${pluginName}BackupRequest(BackupRequest backupRequest) {
		try {
			return new ${pluginName}BackupRequest(backupRequest);
		} catch (JsonProcessingException e) {
			throw new ServiceException(HttpStatus.BAD_REQUEST);
		}
	}

	@Override
	public void restore(IProgressStatus status, RestoreRequest restoreRequest)
			throws ServiceException, InterruptedException {
		// Transform and validate the input
		${pluginName}RestoreRequest request = validateInput(status, restoreRequest);

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
	
	private void receiveBackup(IProgressStatus status, ${pluginName}RestoreRequest request, RestoreContext restoreContext) {
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

	private void doRestoreFullBackup(IProgressStatus status, ${pluginName}RestoreRequest request, RestoreContext context) {
		//TODO Perform a restoration step with the full backup using a technique or tool desirable
		// for the application for which this plugin is developed. Conceptually, it involves taking
		// the full backup data from the staged area as designated by the context.dataDirPath
		// field, and then applying them in a way that is appropriate for the application.
	}

	private void applyIncrementalBackupInSequence(IProgressStatus status, ${pluginName}RestoreRequest request, RestoreContext restoreContext) {
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
	
	private void doRestoreIncrBackup(IProgressStatus status, ${pluginName}RestoreRequest request, RestoreContext context, File incrBackupDir) {
		//TODO Perform a restoration step with the incremental backup using a technique or tool desirable
		// for the application for which this plugin is developed. Conceptually, it involves taking the
		// incremental backup data from the staged area as designated by the incrBackupDir parameter,
		// and then applying them in a way that is appropriate for the application.
		// Note that incremental backup is applied only after full backup has been successfully applied.
	}

	private ${pluginName}RestoreRequest validateInput(IProgressStatus status, RestoreRequest restoreRequest) {
		try {
			// Validate the JSON parsing (deserialization) of the ${pluginName} specific part of the request
			${pluginName}RestoreRequest request = to${pluginName}RestoreRequest(restoreRequest);
			// Basic validation of the input fields
			request.validate(validator);
			//TODO If the plugin needs to perform additional (typically, non-syntactic) validation
			// on the request data, add them here.
			return request;
		} catch (Exception e) {
			logger.error("Input validation failed", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_INPUT_VALIDATION_FAILED, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}

	private ${pluginName}RestoreRequest to${pluginName}RestoreRequest(RestoreRequest restoreRequest) {
		try {
			return new ${pluginName}RestoreRequest(restoreRequest);
		} catch (JsonProcessingException e) {
			throw new ServiceException(HttpStatus.BAD_REQUEST);
		}
	}

	private RestoreContext establishRestoreContext(IProgressStatus status, ${pluginName}RestoreRequest request) {
		try {
			return getRestoreContext(request);
		} catch (Exception e) {
			logger.error("Could not prepare for restore operation", e);
			status.statusMessage(new SessionReport(Constant.PLUGIN_NAME, MessageType.ERH_MAJOR, String.format(NLSMessageTemplate.NLS_FAILED_TO_PREPARE_FOR_RESTORE, ExceptionUtil.getMessageForSessionReport(e))));
			throw e;
		}
	}
	
	private RestoreContext getRestoreContext(${pluginName}RestoreRequest request) {
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

}
