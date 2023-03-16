package ${package}.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectVerOptions {

	// This field is required
    private String sourceClient;
    //TODO Specify additional fields you may have

	public String getSourceClient() {
		return sourceClient;
	}
	public void setSourceClient(String sourceClient) {
		this.sourceClient = sourceClient;
	}
	
}
