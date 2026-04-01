package com.tuttime.menu.config;

import com.tuttime.common.dto.ApiRes;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.annotation.PostConstruct;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@io.swagger.v3.oas.annotations.security.SecurityScheme(
		name = "Bearer Authentication",
		type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "bearer"
)

@Configuration
public class OpenApiConfig {

	@PostConstruct
	public void init() {
		// Tells Springdoc to completely ignore these HTTP header names in UI generation
		SpringDocUtils.getConfig()
				.addRequestWrapperToIgnore(
						jakarta.servlet.http.HttpServletRequest.class
				);

		// Alternatively, ignore by parameter name if they aren't strict servlets:
		// SpringDocUtils.getConfig().addAnnotationsToIgnore(SomeCustomAnnotation.class);
	}

	@Bean
	public OpenAPI customOpenAPI() {
		// Force Swagger UI to make calls to the Gateway port (8080) instead of 28001!
		Server gatewayServer = new Server();
		gatewayServer.setUrl("http://173.249.29.61:8080/menu-docs");
		gatewayServer.setDescription("Gateway Route");

		// Create a security requirement pointing to the name used in your annotation above
		io.swagger.v3.oas.models.security.SecurityRequirement securityRequirement =
				new io.swagger.v3.oas.models.security.SecurityRequirement()
						.addList("Bearer Authentication"); // MUST MATCH THE NAME IN YOUR ANNOTATION EXACTLY

		return new OpenAPI()
				.servers(List.of(gatewayServer)) // Apply the server mapping
				.addSecurityItem(securityRequirement) // <-- THIS TELLS SWAGGER TO SEND THE TOKEN ON REQUESTS!
				.info(new Info()
						.title("Menu Service API")
						.version("1.0")
						.description("API for Menu Management"));
	}

	@Bean
	public OperationCustomizer customizer() {
		return (operation, handlerMethod) -> {
			List<Parameter> parameters = operation.getParameters();
			if (parameters != null) {
				// Remove the headers globally so they don't show up in Swagger UI
				parameters.removeIf(parameter ->
						"X-User-Id".equalsIgnoreCase(parameter.getName()) ||
								"X-User-Roles".equalsIgnoreCase(parameter.getName())
				);
			}
			return operation;
		};
	}
}
