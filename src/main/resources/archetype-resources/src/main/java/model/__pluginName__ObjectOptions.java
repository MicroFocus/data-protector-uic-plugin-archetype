package ${package}.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mf.dp.uic.model.ObjectOptions;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${pluginName}ObjectOptions extends ObjectOptions {
    //TODO Specify additional fields needed by the plugin
}
