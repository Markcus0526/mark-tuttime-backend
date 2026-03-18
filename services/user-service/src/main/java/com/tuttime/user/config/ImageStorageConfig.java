package com.tuttime.user.config;

import com.tuttime.user.service.ImageStorageService;
import com.tuttime.user.service.impl.LocalImageStorageServiceImpl;
import com.tuttime.user.service.impl.S3ImageStorageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Image Storage Service
 * 
 * <p>Selects the appropriate ImageStorageService implementation based on configuration.
 * Supports both local file system storage and S3-compatible storage.
 */
@Slf4j
@Configuration
public class ImageStorageConfig {

    @Value("${app.image.storage.type:s3}")
    private String storageType;

    @Bean
    @Primary
    public ImageStorageService imageStorageService(
            LocalImageStorageServiceImpl localImageStorageService,
            S3ImageStorageServiceImpl s3ImageStorageService) {
        
        log.info("Configuring image storage service. Type: {}", storageType);
        
        if ("s3".equalsIgnoreCase(storageType)) {
            log.info("Using S3 image storage service");
            return s3ImageStorageService;
        } else if ("local".equalsIgnoreCase(storageType)) {
            log.info("Using local file system image storage service");
            return localImageStorageService;
        } else {
            log.warn("Unknown storage type: {}. Defaulting to S3", storageType);
            return s3ImageStorageService;
        }
    }

    /**
     * RestTemplate bean for REST client communication
     * 
     * <p>Provides a RestTemplate instance for inter-service communication.
     * This bean is available for any REST clients that need it.
     * Configured with appropriate timeouts for HTTP operations.
     */
    @Bean
    public RestTemplate restTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(10000); // 10 seconds
        return new RestTemplate(factory);
    }
}

