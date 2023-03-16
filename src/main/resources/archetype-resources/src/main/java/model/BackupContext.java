package ${package}.model;

public class BackupContext {

	private String dataDirPath;
	private String logDirPath;
	private ObjectOptions objectOptions;
	private ObjectVerOptions objectVerOptions;
    //TODO Specify additional fields you may need

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
	public ObjectOptions getObjectOptions() {
		return objectOptions;
	}
	public void setObjectOptions(ObjectOptions objectOptions) {
		this.objectOptions = objectOptions;
	}
	public ObjectVerOptions getObjectVerOptions() {
		return objectVerOptions;
	}
	public void setObjectVerOptions(ObjectVerOptions objectVerOptions) {
		this.objectVerOptions = objectVerOptions;
	}
		
}
