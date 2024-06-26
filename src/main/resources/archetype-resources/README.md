# ${pluginName} Plugin

<h2>Project Structure</h2>

- *src/assembly* - Contains files used by *Maven Assembly Plugin* to build a distribution
- *src/in-project-repo* - Maven local repository to be used to locate UIC jar during build
- *src/main/java* - Java source files
- *src/main/resources* - Resource files to be packaged with the plugin jar
    - *src/main/resources/${pluginNameLowerCase}.properties* - Configuration properties with default values must be defined in this file. In contrast, the *src/assembly/dist/config/${pluginNameLowerCase}.properties* file is not packaged in the plugin jar, and upon installation to target system, can be used to override the default values with the site-specific custom values if any.
    - *src/main/resources/META-INF/schema/backup_appOptions.schema.json* - This file defines JSON schema for the `appOptions` part of the backup specification and backup request.
    - *src/main/resources/META-INF/schema/restore_appOptions.schema.json* - This file defines JSON schema for the `appOptions` part of the restore request.
    - *src/main/resources/META-INF/services/com.mf.dp.uic.plugin.spi.BackupProvider* - This file specifies the full name of the class implementing the service interface (SPI)

<h2>Prerequisites</h2>

- JDK 17 
- Maven 3.6.3 or higher
- Install and configure Data Protector 24.2 or above with at least one UIC instance.
  To install UIC instance on your client machine, refer the Data Protector documentation.
- Copy the UIC jar (integration-controller.jar) in the */opt/omni/unifIntegController/sdk* directory from the client machine where UIC instance is installed, and place it in *src/in-project-repo/com/mf/dp/integration-controller/23.3* directory of this project. The *sdk* directory also contains javadoc (integration-controller-24.2-javadoc.jar) that documents the classes in the UIC jar. Copy it over to the development system for reference as well.

<h2>Develop Plugin</h2>

1. Start by searching all Java source files (*.java) for the occurrences of `TODO` in order to get an understanding of where to focus efforts. Also, see [SampleFS Plugin](https://github.com/MicroFocus/data-protector-uic-plugin-archetype/tree/main/examples/samplefs-plugin) project for an example of developing a simple and functioning plugin based on the skeleton project generated from the archetype. The following lists the most essential places requiring modification:

    - doFullBackup method in ${pluginName}BackupProvider.java
    - doIncrBackup method in ${pluginName}BackupProvider.java
    - doRestoreFullBackup method in ${pluginName}BackupProvider.java
    - doRestoreIncrBackup method in ${pluginName}BackupProvider.java

> Note: The skeleton project is provided to help facilitate and speed up the development. Although not strictly required, it is **strongly** recommended to use it.

2. Look for `TODO` in pom.xml for the placeholder where you should specify 3rd party dependencies that are not already provided by UIC. To find out which dependencies are included in UIC, use Maven (e.g., `mvn dependency:tree`) or your IDE feature for browsing dependency hierarchy.

> Note: The UIC is not a general-purpose container capable of loading multiple plugins without ever inflicting potential class loading issues among the plugins and UIC. Specifically, it does not offer the type of custom class loaders you find in Jakarta EE or OSGi that are needed to robustly isolate one plugin/application from another and to be able to support different versions of the same dependency. Instead, the prevailing use case for UIC is that it loads and executes only a single plugin on each instance. UIC relies on the built-in JVM class loaders where a single system class loader is used to load all classes from the classpath.  For this reason, a **care** must be taken to ensure that the plugin is packaged in such a way that it would not cause conflict with UIC itself.
<br><br>The structure of the skeleton project automatically takes care of the hard part. It divides the dependencies into two categories by breaking it into parent-pom.xml and pom.xml. The parent-pom specifies all top-level dependencies (and their transitive dependencies by definition) that are provided by the UIC runtime environment. The plugin developer is only required to specify additional dependencies in pom.xml that are not supplied at runtime by UIC. For concrete example, see [SampleFS pom.xml](https://github.com/MicroFocus/data-protector-uic-plugin-archetype/tree/main/examples/samplefs-plugin/pom.xml).

3. Edit *src/main/resources/META-INF/backup_appOptions.schema.json* to describe the additional properties implemented by the plugin. Specifically, look for *app-specific-key-for-spec* fields in the example below. All fields contained in the enclosing *appOptions* object need to be described in the schema.

4. Edit *src/main/resources/META-INF/restore_appOptions.schema.json* to describe the additional properties implemented by the plugin. Specifically, look for *app-specific-key-for-restore* fields in the example below. All fields contained in the enclosing *appOptions* object need to be described in the schema.


<h2>Build</h2>

- Run `mvn clean package`.
- After building project *${pluginName.toLowerCase()}-${version}-dist.tar.gz* will be created inside the target folder.

<h2> Installation </h2>

There are two ways to install the plugins. Customer needs to follow one of these two.
- Push Installation
- Manual Installation

<h2>Registration and Push Installation</h2>

- Refer the product documentation.

<h2>Manual Installation</h2>

Here, installing of plugin will be done without registration. In manual installation also plugins will be loaded. But, we can't view the installed plugins in the UI.
- Copy the built distribution (*${pluginName.toLowerCase()}-${version}-dist.tar.gz*) in the *target* directory to a DP client machine where the compatible version of the *Unified Agent* is already installed and fully functioning.
- Make sure that the *dpuic* service is stopped.
- Untar the distribution and copy the content as follows:
    - Copy *${artifactId}.jar* to */opt/omni/unifIntegController/plugins/*
    - Copy the content of *config* directory to */etc/opt/omni/client/modules/unifIntegController/config/*
    - Copy whatever else is needed by the plugin
- Edit */etc/opt/omni/client/modules/unifIntegController/config/dpuic.properties* and specify ```${package}.*``` to the `controller.plugin.packages` property. Uncomment the property if it is commented out. The result should look as follows:
```
controller.plugin.packages=com.mf.dp.sample.*
```
- start *dpuic* service.


Once the *Installation* step is done. Do verify that /var/opt/omni/log/unifIntegController/dpuic.log shows an entry like the following, which is an indication that the plugin was loaded properly:

```
<timestamp> [main] [INFO ] com.mf.dp.uic.plugin.PluginManager - Plugins loaded:
	Plugin(provider=${package}.${pluginName}BackupProvider, name=${pluginNameLowerCase}, title=${pluginName} Plugin, vendor=${groupId}, version=${version}, UICVersion=23.3, UICSPIVersion=1.0.0)
```

> If MongoDB plugin was installed during UIC installation, it is not strictly required to remove it before testing the custom plugin. Deploying multiple plugins simultaneously within the same UIC instance is not ideal or even realistic. In production system, it is **strongly** discouraged to load more than one plugin into a UIC instance.

<h2>(Optional) Plugin Specific REST API</h2>

- Point your browser at *https://&lt;UIC_hostname&gt;:3612/swagger-ui.html* to access swagger UI. If your plugin implements optional REST APIs and they are documented using swagger (as shown in the [SampleFS](https://github.com/MicroFocus/data-protector-uic-plugin-archetype/blob/main/examples/samplefs-plugin/src/main/java/com/mf/dp/sample/fs/rest/SampleFSController.java) example), they should be displayed in the swagger UI.
- Note that plugin-specific REST APIs are NOT required by the SDK and whether it adds value or not is solely to the discretion of the plugin developer. Generally speaking, plugin specific REST APIs are useful only for built-in plugins.

<h2>Creating backup specification</h2>

Use the REST API offered by the Data Protector app server to create/save backup specification. Make sure you have appropriate permissions assigned. Read the REST API documentation on how to obtain auth tokens to invoke the APIs. The endpoint is *https://&lt;CM_hostname&gt;:7116/dp-protection/restws/unified/v1/backupspecifications*. Currently, spec creation is not possible through the Web UI. To see the schemas (data models) associated with the backup specification, access Swagger UI at *https://&lt;CM_hostname&gt;:7116/dp-apis* and look for *UnifiedBackupSpecification*.

Here's an example payload.

	{
	  "specificationName": "${pluginNameLowerCase}-backup-spec-1",
	  "client": {
	    "appHost": "sles15.newton.novell.com",
	    "application": {
	      "type": "unifiedAgent",
	      "subType": "${pluginNameLowerCase}"
	    },
	    "appName": "myAppName",
	    "appOptions": {
	        "appName": "myAppName",
	        "appId": "myAppId",
	        "app-specific-key-for-spec-1": "app-specific-value-for-spec-1",
	        "app-specific-key-for-spec-2": "app-specific-value-for-spec-2",
	        "app-specific-key-for-spec-n": "app-specific-value-for-spec-n"
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

- The `client.application.subType` must be *${pluginNameLowerCase}* (with small case!). This value should be in small case and must exactly match the plugin name.
- The `client.appOptions.appId` should be an ID value that uniquely identifies the application or the data source. An ID is unique for each data source. ID should not contain space.
- The `client.appOptions.appName` should be a name given to the application or the data source. A name is not necessarily unique for each data source. Name should not contain space.
- The `client.appName` must be the same as `client.appOptions.appName`.
- The `client.appOptions.app-specific-key-for-spec-n` are the extensible part of the JSON and their semantics are known only to the plugin.
- The content of `client.appOptions` must be defined in *backup_appOptions.schema.json* explained earlier.
- To get the details required for `target.devices`, go to the `Devices & Media` context with the Data Protector Manager, and consult the information available under `Environment->Devices`.

<h2>Triggering backup request</h2>

There are several ways to trigger a backup request
- Use the REST API (served by app server) to make a backup request programmatically
- Use the CLI omnib command to initiate a backup request interactively

Here is an example request URL and associated payload/body to the app server REST API to programmatically trigger a backup request. Note that the *specificationName* value should match the name of the backup specification from the earlier example.

	https://<CM_hostname>:7116/dp-protection/restws/unified/v1/backup
	
	{
	    "specificationName": "${pluginNameLowerCase}-backup-spec-1",
	    "appSubType": "${pluginNameLowerCase}",
	    "mode": "full",
	    "load": "high",
	    "monitor": "show"
	}

<h2>Triggering restore request</h2>

Use the REST API offered by the app server to trigger a restore request. Currently, restore request cannot be initiated from the Web UI.

Here is an example request URL and associated payload/body to the app server to trigger a restore request. Compare the values with those in the example backup specification shown earlier.

	POST https://<CM_hostname>:7116/dp-protection/restws/unified/v1/restore

	{
	  "barhost": "sles15.newton.novell.com",
	  "appType": "${pluginNameLowerCase}",
	  "appOptions": {
	    "sessionId": "2023/01/24-2",
	    "host": "sles15.newton.novell.com",
	    "appName": "myAppName",
	    "appId": "myAppId",
	    "app-specific-key-for-restore-1": "app-specific-value-for-restore-1",
	    "app-specific-key-for-restore-2": "app-specific-value-for-restore-2",
	    "app-specific-key-for-restore-n": "app-specific-value-for-restore-n"
	  },
	  "report": "critical",
	  "monitor": "show",
	  "ownerName": "",
	  "ownerGroup": ""
	}


A few key points to pay attention to:

- The `appType` must be *${pluginNameLowerCase}* (with small case!)
- Within the `appOptions`, the `appName` and `appId` must match the values given to the corresponding backup specification respectively.
- The `appOptions/app-specific-key-for-restore-n` are the extensible part of the JSON and their semantics are known only to the plugin.
- The content of `appOptions` must be defined in *restore_appOptions.schema.json* explained earlier.
