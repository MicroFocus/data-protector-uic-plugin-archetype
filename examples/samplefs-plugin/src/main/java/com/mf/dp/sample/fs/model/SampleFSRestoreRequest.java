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

import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.dp.uic.exception.ServiceException;
import com.mf.dp.uic.model.RestoreRequest;
import com.mf.dp.uic.model.RestoreRequestCore;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class SampleFSRestoreRequest extends RestoreRequestCore {

	private static final ObjectMapper mapper = new ObjectMapper();

	@NotNull
	@Valid
	@JsonProperty(required = true, value = "appOptions")
	@Schema(requiredMode = RequiredMode.REQUIRED, name = "appOptions", example = "{\"appName\":\"Home\", \"appId\":\"sles15.provo.novell.com\"}")
	private RestoreAppOptions appOptions;

	public SampleFSRestoreRequest(RestoreRequest restoreRequest) throws JsonProcessingException {
		this.setAppHost(restoreRequest.getAppHost());
		this.setSessionId(restoreRequest.getSessionId());
		this.setBarlist(restoreRequest.getBarlist());
		
		this.setAppOptions(mapper.readValue(restoreRequest.getAppOptions().toString(), RestoreAppOptions.class));
	}

	public void validate(Validator validator) throws ServiceException {
		// Bean validation
        Errors errors = new BeanPropertyBindingResult(this, getClass().getName());
		validator.validate(this, errors);
		if (!errors.getAllErrors().isEmpty()) {
			throw new ServiceException (HttpStatus.BAD_REQUEST, errors.getAllErrors().toString());
		}
	}
	
	public RestoreAppOptions getAppOptions() {
		return appOptions;
	}
	public void setAppOptions(RestoreAppOptions appOptions) {
		this.appOptions = appOptions;
	}
}
