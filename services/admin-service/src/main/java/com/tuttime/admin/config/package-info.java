/**
 * Admin Service Configuration
 *
 * <p>This package contains Spring Boot configuration classes for the Admin Service.
 *
 * <h3>Configuration Classes:</h3>
 * <ul>
 *   <li><strong>{@link com.tuttime.admin.config.SecurityConfig}</strong> - Spring Security configuration</li>
 *   <li><strong>{@link com.tuttime.admin.config.GrpcClientConfig}</strong> - gRPC client configuration</li>
 *   <li><strong>{@link com.tuttime.admin.config.WebClientConfig}</strong> - WebClient configuration</li>
 *   <li><strong>{@link com.tuttime.admin.config.RateLimitingConfig}</strong> - Rate limiting configuration</li>
 *   <li><strong>{@link com.tuttime.admin.config.CacheConfig}</strong> - Caching configuration</li>
 *   <li><strong>{@link com.tuttime.admin.config.AsyncConfig}</strong> - Async execution configuration</li>
 *   <li><strong>{@link com.tuttime.admin.config.CorsConfig}</strong> - CORS configuration</li>
 *   <li><strong>{@link com.tuttime.admin.config.OpenApiConfig}</strong> - Swagger/OpenAPI configuration</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Configuration Separation</strong> - Each concern has its own config class</li>
 *   <li><strong>Profile-Based</strong> - Use Spring profiles for environment-specific config</li>
 *   <li><strong>Externalized</strong> - Configuration values come from application.yml</li>
 * </ul>
 */
package com.tuttime.admin.config;

