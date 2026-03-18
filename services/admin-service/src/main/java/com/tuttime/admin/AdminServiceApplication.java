package com.tuttime.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Admin Service Application Entry Point
 *
 * <p>Main Spring Boot application class for the Admin Service microservice.
 * Provides admin panel backend API for managing users, mining, tokens, staking,
 * referrals, conversions, and system configuration.
 */
@SpringBootApplication(
        scanBasePackages = "com.tuttime",
        exclude = {
                CacheAutoConfiguration.class
        }
)
@EnableDiscoveryClient
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EntityScan("com.tuttime.admin.entity")
@EnableJpaRepositories("com.tuttime.admin.repository")
public class AdminServiceApplication {

    /**
     * Main method to start the Admin Service application.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}

