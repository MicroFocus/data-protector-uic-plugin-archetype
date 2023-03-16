package ${package}.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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

public class ${pluginName}RestoreRequest extends RestoreRequestCore {

	private static final ObjectMapper mapper = new ObjectMapper();

	@NotNull
	@Valid
	@JsonProperty(required = true, value = "appOptions")
	@Schema(requiredMode = RequiredMode.REQUIRED, name = "appOptions", example = "{\"appName\":\"Home\", \"appId\":\"sles15.provo.novell.com\"}")
	private RestoreAppOptions appOptions;

	public ${pluginName}RestoreRequest(RestoreRequest restoreRequest) throws JsonProcessingException {
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
