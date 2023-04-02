package ${package}.model;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestoreAppOptions {

	// NOTE: This is a duplicate of the session ID at the top level, except that
	//       the value at the top level is incorrect due to extraneous backslashes
	//       added by poco library (from C++ side) during JSON serialization.
	//       At some point, we will want to clean up this small mess (i.e. fix
	//       the serialization and remove duplicate session ID from appOptions).
	@NotEmpty
	@JsonProperty(required = true, value = "sessionId")
	@Schema(requiredMode = RequiredMode.REQUIRED, example = "2022/07/12-3")	
	private String sessionId;

    @NotEmpty
	@JsonProperty(required = true, value = "appName")
	@Schema(requiredMode = RequiredMode.REQUIRED, example = "")	
	private String appName;
    
    @NotEmpty
	@JsonProperty(required = true, value = "appId")
	@Schema(requiredMode = RequiredMode.REQUIRED, example = "")	
	private String appId;

    //TODO Specify additional fields needed by the plugin

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

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
