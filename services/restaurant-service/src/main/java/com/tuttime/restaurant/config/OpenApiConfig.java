package com.tuttime.restaurant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.annotation.PostConstruct;
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
		gatewayServer.setUrl("http://localhost:8080/restaurant-docs");
		gatewayServer.setDescription("Gateway Route");

		return new OpenAPI()
				.servers(List.of(gatewayServer)) // Apply the server mapping
				.info(new Info()
						.title("Restaurant Service API")
						.version("1.0")
						.description("API for Restaurant Management"));
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
