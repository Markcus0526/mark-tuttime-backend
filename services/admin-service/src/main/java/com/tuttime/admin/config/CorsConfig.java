package com.tuttime.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration for Admin Service
 * 
 * <p>NOTE: CORS is handled by admin-gateway, not by admin-service.
 * When admin-service is accessed through admin-gateway (which is the normal case),
 * the gateway adds CORS headers. If admin-service also adds CORS headers,
 * it results in duplicate Access-Control-Allow-Origin headers, causing CORS errors.
 * 
 * <p>This configuration is disabled by default. If you need to access admin-service
 * directly (bypassing the gateway), enable CORS by setting:
 * app.cors.enabled=true
 */
@Configuration
public class CorsConfig {
    
    @Value("${app.cors.enabled:false}")
    private boolean corsEnabled;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // CORS is disabled by default - handled by admin-gateway
        // Only enable if accessing admin-service directly (not through gateway)
        if (corsEnabled) {
            CorsConfiguration configuration = new CorsConfiguration();
            
            // Parse allowed origins
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            configuration.setAllowedOrigins(origins);
            
            // Parse allowed methods
            List<String> methods = Arrays.asList(allowedMethods.split(","));
            configuration.setAllowedMethods(methods);
            
            // Parse allowed headers
            if ("*".equals(allowedHeaders)) {
                configuration.addAllowedHeader("*");
            } else {
                List<String> headers = Arrays.asList(allowedHeaders.split(","));
                configuration.setAllowedHeaders(headers);
            }
            
            configuration.setAllowCredentials(allowCredentials);
            configuration.setMaxAge(maxAge);
            
            // Exposed headers for frontend
            configuration.setExposedHeaders(Arrays.asList(
                    "Authorization",
                    "Content-Type",
                    "X-Total-Count"
            ));

            source.registerCorsConfiguration("/api/admin/**", configuration);
        } else {
            // Return empty configuration - no CORS headers will be added
            // Gateway handles CORS
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(List.of()); // Empty - no CORS
            source.registerCorsConfiguration("/**", configuration);
        }
        
        return source;
    }
}

