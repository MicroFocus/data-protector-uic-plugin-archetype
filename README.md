# Maven Archetype for Opentext Data Protector UIC Plugin

This project is a [Maven Archetype](https://maven.apache.org/archetype/) project that can be used to create the
skeleton project structure and stub files for the development of *UIC plugin* for *Oentext Data Protector*. 

## Installing the archetype
* Clone or download the project.
* Make sure that you have JDK 17 and recent version of Maven installed.
* Run `mvn clean install` in the root of the project. This will build and install the archetype on the local machine.

## Using the archetype to create a plugin
* cd to a directory where you want to create a new plugin project
* Run `mvn archetype:generate -DarchetypeGroupId=com.microfocus.dp -DarchetypeArtifactId=uic-plugin-archetype -DarchetypeVersion=1.0.0` and enter appropriate input values for the properties when prompted. Here's an example:


&nbsp;&nbsp;&nbsp;&nbsp;**IMPORTANT**:
While specifying the values for the properties, ensure the following:
* Do not include space(' ') or comma(',').
* For pluginName, the value is case insensitive.
* For package, the value should start with the string 'com.mf.dp.sample'.
* For version, the value can contain the numbers and ('.'). Example: "1.0","1.0.0".
* For *pluginNameLowerCase*, do not enter a value. You must accept the default value presented.

![Screenshot](images/generate_project.png?raw=true)


## Using the generated project
Refer to the README.md located in the root of the generated project for instructions on how to use the skeleton project to develop, build, deploy, and run a plugin.

## Example plugin
This project also contains an example plugin named [SampleFS](https://github.com/MicroFocus/data-protector-uic-plugin-archetype/tree/main/examples/samplefs-plugin). The *SampleFS* plugin is developed using the skeleton project created from the archetype and adds rudimentary backup and restore functionality for a directory on the file system.

## What's new
- 1.0.0
    - Initial release - This release is compatible with Data Protector 24.2
    
## License
[MIT license](LICENSE).

