package com.tuttime.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

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
@EnableDiscoveryClient
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}

