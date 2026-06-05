package com.tuttime.review.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.annotation.PostConstruct;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@io.swagger.v3.oas.annotations.security.SecurityScheme(
        name = "Bearer Authentication",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)

@Configuration
@Profile("!production")
public class OpenApiConfig {

    @Value("${springdoc.swagger.gateway.url}")
    private String gatewayUrl;

    @PostConstruct
    public void init() {
        SpringDocUtils.getConfig()
                .addRequestWrapperToIgnore(
                        jakarta.servlet.http.HttpServletRequest.class
                );
    }

    @Bean
    public OpenAPI customOpenAPI() {
        Server gatewayServer = new Server();
        gatewayServer.setUrl(gatewayUrl);
        gatewayServer.setDescription("Gateway Route");

        // Create a security requirement pointing to the name used in your annotation above
        io.swagger.v3.oas.models.security.SecurityRequirement securityRequirement =
                new io.swagger.v3.oas.models.security.SecurityRequirement()
                        .addList("Bearer Authentication"); // MUST MATCH THE NAME IN YOUR ANNOTATION EXACTLY

        return new OpenAPI()
                .servers(List.of(gatewayServer)) // Apply the server mapping
                .addSecurityItem(securityRequirement) // <-- THIS TELLS SWAGGER TO SEND THE TOKEN ON REQUESTS!
                .info(new Info()
                        .title("Review Service API")
                        .version("1.0")
                        .description("API for Review Management"));
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
