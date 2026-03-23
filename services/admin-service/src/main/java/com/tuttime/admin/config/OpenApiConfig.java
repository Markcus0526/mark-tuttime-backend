package com.tuttime.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Configuration
 * 
 * <p>Configures Swagger UI for admin service API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI adminServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Admin Service API")
                        .description("""
                            Comprehensive Admin Panel API for TUTTIME Platform
                            
                            This API provides endpoints for managing users, tokens, mining, staking, conversions, and system configuration.
                            All endpoints require JWT authentication except `/api/admin/auth/login`.
                            
                            **Features:**
                            - User Management (CRUD, KYC, Blocking)
                            - Bulk Operations (up to 1000 users)
                            - Token Management (balances, adjustments, burning)
                            - Mining Management (AP Credits, speed adjustments)
                            - Staking Management
                            - Conversion Management
                            - Analytics & Dashboards
                            - Anti-Fraud Management
                            - Token Security (whitelist/blacklist)
                            - System Configuration
                            
                            **Rate Limits:**
                            - Login: 5 attempts per IP per 15 minutes
                            - Authenticated endpoints: 100 requests per admin per minute
                            
                            **Roles:**
                            - SUPER_ADMIN: Full access to all endpoints
                            - ADMIN: Standard admin access
                            - MODERATOR: Limited access (user management, content moderation)
                            - VIEWER: Read-only access
                            """)
                        .version("1.0.0")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("TUTTIME Support")
                                .email("support@tuttime.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from `/api/admin/auth/login` endpoint")));
    }
}

