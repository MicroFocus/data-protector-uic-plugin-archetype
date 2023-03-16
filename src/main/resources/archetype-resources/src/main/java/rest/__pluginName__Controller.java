package ${package}.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "${pluginName}")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping(path = "/${pluginNameLowerCase}")
public class ${pluginName}Controller {

	//TODO Add methods implementing optional REST APIs that this plugin may provide

}
