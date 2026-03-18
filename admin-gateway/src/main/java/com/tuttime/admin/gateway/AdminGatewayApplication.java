package com.tuttime.admin.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Admin Gateway Application Entry Point
 *
 * <p>This is the main Spring Boot application class for the Admin Gateway.
 * It acts as a thin Backend-for-Frontend (BFF) layer that routes admin requests
 * to the admin-service and handles admin authentication.
 *
 * <p><b>Architecture:</b>
 * <ul>
 *   <li><b>@SpringBootApplication</b> - Enables Spring Boot auto-configuration</li>
   * </ul>
   *
 * <p><b>Gateway Responsibilities:</b>
 * <ul>
 *   <li>Admin authentication (login/logout)</li>
 *   <li>Request routing to admin-service</li>
 *   <li>Simple data aggregation when needed</li>
 *   <li>Rate limiting for admin endpoints</li>
 *   <li>CORS configuration</li>
 * </ul>
 *
 * <p><b>Note:</b> This gateway does NOT contain business logic.
 * All admin business logic is in admin-service.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AdminGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminGatewayApplication.class, args);
    }
}

