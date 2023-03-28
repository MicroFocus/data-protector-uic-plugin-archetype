# SampleFS Plugin

## Project Structure

- *src/assembly* - Contains files used by *Maven Assembly Plugin* to build a distribution
- *src/in-project-repo* - Maven local repository to be used to locate UIC jar during build
- *src/main/java* - Java source files
- *src/main/resources* - Resource files to be packaged with the plugin jar
    - *src/main/resources/samplefs.properties* - Configuration properties with default values must be defined in this file. In contrast, the *src/assembly/dist/config/samplefs.properties* file is not packaged in the plugin jar, and upon installation to target system, can be used to override the default values with the site-specific custom values if any.
    - *src/main/resources/META-INF/schema/backup_appOptions.schema.json* - This file defines JSON schema for the `appOptions` part of the backup specification and backup request.
    - *src/main/resources/META-INF/schema/restore_appOptions.schema.json* - This file defines JSON schema for the `appOptions` part of the restore request.
    - *src/main/resources/META-INF/services/com.mf.dp.uic.plugin.spi.BackupProvider* - This file specifies the full name of the class implementing the service interface (SPI)

## Preparation

- JDK 17 and recent version of Maven are required
- Install and configure Data Protector 23.3 with at least one UIC instance.
- Copy the UIC jar (integration-controller-23.3.jar) in the */opt/omni/unifIntegController/sdk* directory from the client machine where UIC instance is installed, and place it in *src/in-project-repo/com/mf/dp/integration-controller/23.3* directory of this project. The version of the UIC jar (23.3) must match the value of *project.parent.version* element in pom.xml of this project. The *sdk* directory also contains javadoc (integration-controller-23.3-javadoc.jar) that documents the classes in the UIC jar. Copy it over to the development system for reference as well.

## Build

Run `mvn clean package`

## Manual Installation

- Copy the built distribution (*samplefs-plugin-1.0.0-dist.tar.gz*) in the *target* directory to a DP client machine where the compatible version of the *Unified Agent* is already installed and fully functioning.
- Make sure that the *dpuic* service is stopped.
- Untar the distribution and copy the content as follows:
    - Copy *samplefs-plugin.jar* to */opt/omni/unifIntegController/plugins/*
    - Copy the content of *config* directory to */etc/opt/omni/client/modules/unifIntegController/config/*
    - Copy bin directory (yes, the entire directory) to /opt/omni/unifIntegController/ (After copy, you should have /opt/omni/unifIntegController/bin/copy_files.sh). Check the file permissions on the copied files to ensure that the scripts are executable. 
- Edit */etc/opt/omni/client/modules/unifIntegController/config/dpuic.properties* and specify `com.mf.dp.sample.*` to the `controller.plugin.packages` property. Uncomment the property if it is commented out. The result should look as follows:

```
controller.plugin.packages=com.mf.dp.sample.*
```
- Start *dpuic* service. Verify that /var/opt/omni/log/unifIntegController/dpuic.log shows an entry like the following:

```
<timestamp> [main] [INFO ] com.mf.dp.uic.plugin.PluginManager - Plugins loaded:
	Plugin(provider=com.mf.dp.sample.fs.SampleFSBackupProvider, name=SampleFS, title=SampleFS Plugin, vendor=Micro Focus, version=1.0.0, UICVersion=23.3, UICSPIVersion=1.0.0)
```

> It is not strictly required to remove the built-in *MongoDB Plugin* from the *Unified Integration Controller* before testing the custom plugin. Multiple plugins can load and run simultaneously within the same UIC instance **as long as** their respective dependencies do not cause conflict with each other, although such deployment is not ideal or even realistic. In production system, it is **strongly** discouraged to load more than one plugin into a UIC instance.

## Plugin Specific REST API
- Point your browser at *https://&lt;UIC_hostname&gt;:3612/swagger-ui.html* to access swagger UI
- You can see that SampleFS implements and exposes one REST API even though it is not used anywhere. Note that plugin-specific REST APIs are NOT required by the SDK and whether it adds value or not is solely to the discretion of the plugin developer. Generally speaking, plugin specific REST APIs could be made useful only for built-in plugins. For this sample, the sole purpose of the REST API is to illustrate how one could be written if so desired. 

## Creating backup specification

Use the REST API offered by the app server to create/save a backup specification. The endpoint is *https://&lt;CM_hostname&gt;:7116/dp-protection/restws/unified/v1/backupspecifications*. Currently, spec creation is not possible through the Web UI.

Here's an example payload.

	{
	  "specificationName": "samplefs-backup-spec-1",
	  "client": {
	    "appHost": "sles15.newton.novell.com",
	    "application": {
	      "type": "unifiedAgent",
	      "subType": "SampleFS"
	    },
	    "appName": "home_test",
	    "appOptions": {
	        "appName": "home_test",
	        "appId": "sles15.newton.novell.com",
	        "dirPath": "/home/test"
	    },
	    "args": {
	      "readstdin": true
	    },
	    "executable": "unified_bar_executor",
	    "protection": {
	      "type": "Permanent",
	      "until": 1,
	      "date": "2023-12-03T04:59:59.999Z"
	    },
	    "report": "Warning",
	    "isPublic": false,
	    "isProfileEnabled": false,
	    "isCompressionEnabled": false
	  },
	  "target": {
	    "loadBalancing": {
	      "min": 1,
	      "max": 5
	    },
	    "devices": [
	      {
	        "name": "DPFileLibrary_Writer0",
	        "type": "File Library"
	      }
	    ]
	  },
	  "dataSecurity": "none",
	  "owner": {
	    "userName": "",
	    "group": "",
	    "client": ""
	  },
	  "postExec": {
	    "script": "",
	    "host": ""
	  },
	  "preExec": {
	    "script": "",
	    "host": ""
	  }
	}

A few key points to pay attention to:

- The `client.application.subType` must be *SampleFS* (with exact case!). This value is case sensitive and must exactly match the plugin name.
- The `client.appOptions.appId` should be an ID value that uniquely identifies the application or the data source. An ID is unique for each data source. ID should not contain space.
<br>In the case of *SampleFS*, the data source is the file systems on the machine on which the plugin is installed. Therefore, the client's hostname is most appropriate as the ID.
- The `client.appOptions.appName` should be a name given to the application or the data source. A name is not necessarily unique for each data source. Name should not contain space.
- The `client.appName` must be the same as `client.appOptions.appName`.
- The `client.appOptions.dirPath` must denote an existing directory (not a file) and this implementation accepts only a single value (hence, not really useful from the real-world feature perspective).

## Triggering backup request

There are several ways to trigger a backup request
- Use the REST API (served by app server) to make a backup request programmatically
- Use the Web UI to initiate a backup request interactively
- Use the Web UI to set up scheduled backup

Here is an example request URL and associated payload/body to the app server REST API to programmatically trigger a backup request. Note that the *specificationName* value should match the name of the backup specification from the earlier example.

	https://<CM_hostname>:7116/dp-protection/restws/unified/v1/backup
	
	{
	    "specificationName": "samplefs-backup-spec-1",
	    "mode": "full",
	    "load": "high",
	    "monitor": "show"
	}

*SampleFS* supports both full (*"full"*) and incremental (*"incr"*) backups.

## Triggering restore request

Use the REST API offered by the app server to trigger a restore request. Currently, restore request cannot be initiated from the Web UI.

Here is an example request URL and associated payload/body to the app server REST API to programmatically trigger a restore request. Compare the values with those in the example backup specification shown earlier.

	https://<CM_hostname>:7116/dp-protection/restws/unified/v1/restore

	{
	  "barhost": "sles15.newton.novell.com",
	  "appType": "SampleFS",
	  "appOptions": {
	    "sessionId": "2023/01/24-2",
	    "host": "sles15.newton.novell.com",
	    "appName": "home_test",
	    "appId": "sles15.newton.novell.com",
	    "restoreDirPath": "/home_restore_test"
	  },
	  "report": "critical",
	  "monitor": "show",
	  "ownerName": "",
	  "ownerGroup": ""
	}

A few key points to pay attention to:

- The `appType` must be *SampleFS* (with exact case!)
- Within the `appOptions`, the `appName` and `appId` must match the values given to the corresponding backup specification respectively.
- The `restoreDirPath` field must be given a value that denotes an existing and empty directory. The (required) full backup data is restored directly to the specified directory. The (optional) incremental backups, if any in the chain, are first restored to the temporary area (under */var/opt/omni/tmp/samplefs/restore/&lt;timestamp&gt;*) and then applied to the *restoreDirPath* directory one by one in ascending time order within the chain. 

> Each incremental backup captures only newly added or updated files since the last backup. It does not capture information about deleted files. Consequently, when you restore from a backup chain, the end state of the restored area may not be identical to the current state of the source area because the restored area still contains those files that have been gone from the source area. This is not a full feature production grade plugin.
