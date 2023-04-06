package ${package};

import java.io.File;

import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

import com.mf.dp.uic.plugin.spi.IProgressStatus;

import ${package}.model.BackupContext;
import ${package}.model.RestoreContext;
import ${package}.model.${pluginName}BackupRequest;
import ${package}.model.${pluginName}LastBackupDetail;
import ${package}.model.${pluginName}RestoreRequest;

@Component
@PropertySources({
    @PropertySource("classpath:${pluginNameLowerCase}.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="classpath:config/${pluginNameLowerCase}.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:./${pluginNameLowerCase}.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:./config/${pluginNameLowerCase}.properties"),
    @PropertySource(ignoreResourceNotFound=true, value="file:${dpuic.config.dirpath}/${pluginNameLowerCase}.properties"),
})
public class ${pluginName}BackupProvider extends BaseBackupProvider {
	
	//TODO Autowire additional services you may have defined

	@Override
	protected void doFullBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context) {
		//TODO Perform full backup using a technique or tool desirable for the application
		// for which this plugin is developed. Conceptually, it involves obtaining the latest
		// and consistent state of the application data, and then staging them in the prepared
		// data directory as designated by the context.dataDirPath field.
	}
	
	@Override
	protected void doIncrBackup(IProgressStatus status, ${pluginName}BackupRequest request, BackupContext context, ${pluginName}LastBackupDetail lastBackupDetail) {
		//TODO Perform incremental backup using a technique or tool desirable for the application
		// for which this plugin is developed. Conceptually, it involves obtaining the application
		// data delta that have changed since the last time it was backed up, and then staging
		// them in the prepared log directory as designated by the context.logDirPath field.
	}

	@Override
	protected void doRestoreFullBackup(IProgressStatus status, ${pluginName}RestoreRequest request, RestoreContext context) {
		//TODO Perform a restoration step with the full backup using a technique or tool desirable
		// for the application for which this plugin is developed. Conceptually, it involves taking
		// the full backup data from the staged area as designated by the context.dataDirPath
		// field, and then applying them in a way that is appropriate for the application.
	}
	
	@Override
	protected void doRestoreIncrBackup(IProgressStatus status, ${pluginName}RestoreRequest request, RestoreContext context, File incrBackupDir) {
		//TODO Perform a restoration step with the incremental backup using a technique or tool desirable
		// for the application for which this plugin is developed. Conceptually, it involves taking the
		// incremental backup data from the staged area as designated by the incrBackupDir parameter,
		// and then applying them in a way that is appropriate for the application.
		// Note that incremental backup is applied only after full backup has been successfully applied.
	}
}
