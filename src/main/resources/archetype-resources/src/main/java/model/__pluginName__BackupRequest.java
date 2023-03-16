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
import com.mf.dp.uic.model.BackupRequest;
import com.mf.dp.uic.model.BackupRequestCore;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public class ${pluginName}BackupRequest extends BackupRequestCore {

	private static final ObjectMapper mapper = new ObjectMapper();

	@NotNull
	@Valid
	@JsonProperty(required = true, value = "appOptions")
	@Schema(requiredMode = RequiredMode.REQUIRED, name = "appOptions")
	private BackupAppOptions appOptions;

	public ${pluginName}BackupRequest(BackupRequest backupRequest) throws JsonProcessingException {
		this.setAppHost(backupRequest.getAppHost());
		this.setConcurrency(backupRequest.getConcurrency());
		this.setSessionId(backupRequest.getSessionId());
		this.setBackupType(backupRequest.getBackupType());
		this.setBarlist(backupRequest.getBarlist());
		
		this.setAppOptions(mapper.readValue(backupRequest.getAppOptions().toString(), BackupAppOptions.class));
	}

	public void validate(Validator validator) throws ServiceException {
		// Bean validation
        Errors errors = new BeanPropertyBindingResult(this, getClass().getName());
		validator.validate(this, errors);
		if (!errors.getAllErrors().isEmpty()) {
			throw new ServiceException (HttpStatus.BAD_REQUEST, errors.getAllErrors().toString());
		}
	}
	
	public BackupAppOptions getAppOptions() {
		return appOptions;
	}
	public void setAppOptions(BackupAppOptions appOptions) {
		this.appOptions = appOptions;
	}
}
