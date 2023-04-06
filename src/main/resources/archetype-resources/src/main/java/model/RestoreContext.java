package ${package}.model;

public class RestoreContext {

	private long time;
	private String dataDirPath;
	private boolean deleteDataDirAfterUse = false;
	private String logDirPath;
	private boolean deleteLogDirAfterUse = false;
    //TODO Specify additional fields needed by the plugin
	
	public RestoreContext() {
		this.time = System.currentTimeMillis();
	}
	public long getTime() {
		return time;
	}
	public String getDataDirPath() {
		return dataDirPath;
	}
	public void setDataDirPath(String dataDirPath) {
		this.dataDirPath = dataDirPath;
	}
	public boolean isDeleteDataDirAfterUse() {
		return deleteDataDirAfterUse;
	}
	public void setDeleteDataDirAfterUse(boolean deleteDataDirAfterUse) {
		this.deleteDataDirAfterUse = deleteDataDirAfterUse;
	}
	public String getLogDirPath() {
		return logDirPath;
	}
	public void setLogDirPath(String logDirPath) {
		this.logDirPath = logDirPath;
	}
	public boolean isDeleteLogDirAfterUse() {
		return deleteLogDirAfterUse;
	}
	public void setDeleteLogDirAfterUse(boolean deleteLogDirAfterUse) {
		this.deleteLogDirAfterUse = deleteLogDirAfterUse;
	}

}
