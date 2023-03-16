/*
 * Copyright (c) 2023 Micro Focus or one of its affiliates.
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mf.dp.sample.fs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotEmpty;

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

    @NotEmpty
	@JsonProperty(required = true, value = "restoreDirPath")
	@Schema(requiredMode = RequiredMode.REQUIRED, example = "/home/tom")	
	private String restoreDirPath;

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

	public String getRestoreDirPath() {
		return restoreDirPath;
	}

	public void setRestoreDirPath(String restoreDirPath) {
		this.restoreDirPath = restoreDirPath;
	}
    
}
