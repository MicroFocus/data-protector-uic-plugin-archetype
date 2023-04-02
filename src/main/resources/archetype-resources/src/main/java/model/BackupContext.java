package ${package}.model;

public class BackupContext {

	private String dataDirPath;
	private String logDirPath;
	private ${pluginName}ObjectOptions objectOptions;
	private ${pluginName}ObjectVerOptions objectVerOptions;
    //TODO Specify additional fields needed by the plugin

	public String getDataDirPath() {
		return dataDirPath;
	}
	public void setDataDirPath(String dataDirPath) {
		this.dataDirPath = dataDirPath;
	}
	public String getLogDirPath() {
		return logDirPath;
	}
	public void setLogDirPath(String logDirPath) {
		this.logDirPath = logDirPath;
	}
	public ${pluginName}ObjectOptions getObjectOptions() {
		return objectOptions;
	}
	public void setObjectOptions(${pluginName}ObjectOptions objectOptions) {
		this.objectOptions = objectOptions;
	}
	public ${pluginName}ObjectVerOptions getObjectVerOptions() {
		return objectVerOptions;
	}
	public void setObjectVerOptions(${pluginName}ObjectVerOptions objectVerOptions) {
		this.objectVerOptions = objectVerOptions;
	}
		
}
