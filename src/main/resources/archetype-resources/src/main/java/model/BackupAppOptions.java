package ${package}.model;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackupAppOptions {

    @NotEmpty
	@JsonProperty(required = true, value = "appName")
	@Schema(requiredMode = RequiredMode.REQUIRED, example = "")	
	private String appName;
    
    @NotEmpty
	@JsonProperty(required = true, value = "appId")
	@Schema(requiredMode = RequiredMode.REQUIRED, example = "")	
	private String appId;

    //TODO Specify additional fields you may have

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}
    
}
