package com.tuttime.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * API Gateway Application Entry Point
 *
 * <p>This is the main Spring Boot application class for the API Gateway.
 * It provides a single entry point for all client requests and routes them
 * to the appropriate microservices.
 *
 * <p><b>Configuration:</b>
 * <ul>
 *   <li><b>@SpringBootApplication</b> - Enables Spring Boot auto-configuration</li>
 * </ul>
 *
 * <p><b>Gateway Responsibilities:</b>
 * <ul>
 *   <li>Request routing to microservices</li>
 *   <li>JWT authentication and authorization</li>
 *   <li>Load balancing across service instances</li>
 *   <li>Circuit breaker and resilience patterns</li>
 *   <li>Request/response logging and tracing</li>
 *   <li>CORS handling</li>
 * </ul>
 */

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@Slf4j
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupInfo(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();

        // Modern way to check profiles in Spring 3.x
        boolean isGraylogEnabled = env.acceptsProfiles(Profiles.of("graylog-enabled"));

        log.info("==========================================");
        log.info("Active Profiles: {}", String.join(", ", env.getActiveProfiles()));
        log.info("Graylog Host: {}", env.getProperty("graylog.host", "NOT SET"));
        log.info("Graylog Port: {}", env.getProperty("graylog.port", "NOT SET"));
        log.info("Graylog Status: {}", isGraylogEnabled ? "ENABLED" : "DISABLED");
        log.info("==========================================");
    }
}

