# ${pluginName} Plugin

<h2>Project Structure</h2>

- *src/assembly* - Contains files used by *Maven Assembly Plugin* to build a distribution
- *src/in-project-repo* - Maven local repository to be used to locate UIC jar
- *src/main/java* - Java source files
- *src/main/resources* - Resource files to be packaged with the jar
    - *src/main/resources/${pluginNameLowerCase}.properties* - Configuration properties with default values must be defined in this file. In contrast, the *src/assembly/dist/config/${pluginNameLowerCase}.properties* file, upon installation to target system, can be used to override the default values with custom values if any.
    - *src/main/resources/META-INF/schema/backup_appOptions.schema.json* - This file defines JSON schema for the `appOptions` part of the backup specification and backup request.
    - *src/main/resources/META-INF/schema/restore_appOptions.schema.json* - This file defines JSON schema for the `appOptions` part of the restore request.
    - *src/main/resources/META-INF/services/com.mf.dp.uic.plugin.spi.BackupProvider* - This file specifies the full name of the class implementing the service interface (SPI)

<h2>Preparation</h2>

- JDK 17 and recent version of Maven are required
- Obtain UIC jar (integration-controller-23.3.jar) and place it in *src/in-project-repo/com/mf/dp/integration-controller/23.3* directory. The *UIC version* (23.3) must match the value of `integration-controller.version` property defined in pom.xml.

<h2>Development</h2>

1. Start by searching all Java source files (*.java) for the occurrences of `TODO` in order to get an understanding of where to focus efforts. Also, see [SampleFS Plugin](https://github.com/MicroFocus/data-protector-uic-plugin-archetype/examples/samplefs-plugin) project for an example of developing a simple and functioning plugin based on the skeleton project generated from the archetype.

> Note: The skeleton project is provided to help facilitate and speed up the development. While it is recommended to use it, it is not required.

2. Look for `TODO` in pom.xml for the placeholder where you should specify 3rd party dependencies that are not already provided by UIC. To find out which dependencies are included in UIC, use Maven (e.g., `mvn dependency:tree`) or your IDE.

> Note: The UIC is not a general-purpose container capable of loading multiple plugins without inflicting potential class loading issues among the plugins and UIC. Specifically, it does not offer the type of custom class loaders you find in Jakarta EE or OSGi that are needed to robustly isolate one plugin/application from another and to be able to support different versions of the same dependency. Instead, the prevailing use case for UIC is that it loads and executes only a single plugin on each instance. UIC relies on the built-in JVM class loaders where a single system class loader is used to load all classes from the classpath.  For this reason, a **care** must be taken to ensure that the plugin is packaged in such a way that it would not cause conflict with UIC itself.

3. Edit *src/main/resources/META-INF/backup_appOptions.schema.json* to describe the additional properties implemented by the plugin. Specifically, look for *app-specific-key-for-spec* fields in the example below. Those are the fields that need to be defined in the schema.

4. Edit *src/main/resources/META-INF/restore_appOptions.schema.json* to describe the additional properties implemented by the plugin. Specifically, look for *app-specific-key-for-restore* fields in the example below. Those are the fields that need to be defined in the schema.


<h2>Build</h2>

Run `mvn clean package`

<h2>Manual Installation</h2>

- Copy the built distribution (*${artifactId}-${version}-dist.tar.gz*) in the *target* directory to a DP client machine where the compatible version of the *Unified Agent* is already installed and fully functioning.
- Make sure that the *dpuic* service is stopped
- Untar the distribution and copy the content as follows
    - Copy *${artifactId}.jar* to */opt/omni/unifIntegController/plugins/*
    - Copy the content of *config* directory to */etc/opt/omni/client/modules/unifIntegController/config/*
    - Copy whatever else is needed by the plugin
- Edit */etc/opt/omni/client/modules/unifIntegController/config/dpuic.properties* and specify ```${package},${package}.*``` to the `controller.plugin.packages` property. Uncomment the property if it is commented out. The result should look as follows:
```
controller.plugin.packages=${package},${package}.*
```
- Start *dpuic* service

> It is not strictly required to remove the built-in *MongoDB Plugin* from the *Unified Integration Controller* before testing the custom plugin. Multiple plugins can load and run simultaneously within the same UIC instance **as long as** their respective dependencies do not cause conflict with each other, although such deployment is not ideal or even realistic. In production system, it is **strongly** discouraged to load more than one plugin into a UIC instance.

<h2>(Optional) Plugin Specific REST API</h2>

- Point your browser at *https://&lt;UIC_hostname&gt;:3612/swagger-ui.html* to access swagger UI. If your plugin implements optional REST APIs and they are documented using swagger (as shown in the [SampleFS](https://github.com/MicroFocus/data-protector-uic-plugin-archetype/examples/samplefs-plugin/src/main/java/com/mf/dp/sample/fs/rest/SamplefsController.java) example), they should be shown in the swagger UI.
- Note that plugin-specific REST APIs are NOT required by the SDK and whether it adds value or not is solely to the discretion of the plugin designer.
- To test run the exposed REST API through the swagger UI, do the "Authorize" first to specify the auth token, and then "Try it out". For instructions on how to manually obtain authorization token, consult the Data Protector document.

<h2>Creating backup specification</h2>

Use the REST API offered by the app server to create/save backup specification. The endpoint is *https://&lt;CM_hostname&gt;:7116/dp-protection/restws/unified/v1/backupspecifications*. Currently, spec creation is not possible through the Web UI.

Here's an example payload.

	{
	  "specificationName": "${pluginNameLowerCase}-backup-spec-1",
	  "client": {
	    "appHost": "sles15.newton.novell.com",
	    "application": {
	      "type": "unifiedAgent",
	      "subType": "${pluginName}"
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

- The client.application.subType must be *${pluginName}* (with exact case!). This value is case sensitive and must exactly match the plugin name.
- Within the client.appOptions, the appName and appId could be anything (space is not allowed). However, consider giving them proper name and id that make sense from the plugin design and usage point of view within the Data Protector architecture.
- The client.appOptions.app-specific-key-for-spec-n are the extensible part of the JSON and their semantics are known only to the plugin.

<h2>Triggering backup request</h2>

There are several ways to trigger a backup request
- Use the REST API (served by app server) to make a backup request programmatically
- Use the Web UI to initiate a backup request interactively
- Use the Web UI to set up scheduled backup

Here is an example request URL and associated payload/body to the app server REST API to programmatically trigger a backup request. Compare the values with those in the example backup specification shown earlier.

	https://<CM_hostname>:7116/dp-protection/restws/unified/v1/backup
	
	{
	    "specificationName": "${pluginNameLowerCase}-backup-spec-1",
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
	  "appType": "${pluginName}",
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

- The appType must be *${pluginName}* (with exact case!)
- Within the appOptions, the appName and appId must exactly match the values given to the corresponding backup specification.
- The appOptions/app-specific-key-for-restore-n are the extensible part of the JSON and their semantics are known only to the plugin.