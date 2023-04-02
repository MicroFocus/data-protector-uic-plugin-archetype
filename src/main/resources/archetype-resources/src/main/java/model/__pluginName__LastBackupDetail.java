package ${package}.model;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mf.dp.uic.model.LastBackupDetail;
import com.mf.dp.uic.util.JsonUtil;

public class ${pluginName}LastBackupDetail {

	private ${pluginName}ObjectVerOptions objectVerOptions;
	private String backupType;
	
	public ${pluginName}LastBackupDetail (LastBackupDetail detail) throws JsonProcessingException {
		if(!StringUtils.isBlank(detail.getObjectVerOptionsAsJsonStr()))	
			this.objectVerOptions = JsonUtil.deserializeFromJson(detail.getObjectVerOptionsAsJsonStr(), ${pluginName}ObjectVerOptions.class);
		this.backupType = detail.getBackupType();
	}
	
	public ${pluginName}ObjectVerOptions getObjectVerOptions() {
		return objectVerOptions;
	}
	public void setObjectVerOptions(${pluginName}ObjectVerOptions objectVerOptions) {
		this.objectVerOptions = objectVerOptions;
	}
	public String getBackupType() {
		return backupType;
	}
	public void setBackupType(String backupType) {
		this.backupType = backupType;
	}	
	
}
