package ${package}.model;

public class RestoreContext {

	private String dataDirPath;
	private String logDirPath;
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

}
