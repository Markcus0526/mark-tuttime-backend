package com.tuttime.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * User Service Application Entry Point
 *
 * <p>Main Spring Boot application class for the User Service microservice.
 * Handles user registration, authentication, profile management, and KYC processing.
 */
@SpringBootApplication(
        scanBasePackages = "com.tuttime",
        exclude = {
                // Exclude Spring Boot's default cache auto-configuration
                // Our custom RedisConfig in commons-events will provide the CacheManager
                CacheAutoConfiguration.class
        }
)
@EnableDiscoveryClient
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
@EnableScheduling
@EntityScan("com.tuttime.user.entity")
@EnableJpaRepositories("com.tuttime.user.repository")
@Slf4j
public class UserServiceApplication {

    /**
     * Main method to start the User Service application.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupInfo(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String[] activeProfiles = env.getActiveProfiles();
        String graylogHost = env.getProperty("graylog.host");
        String graylogPort = env.getProperty("graylog.port");
        
        log.info("==========================================");
        log.info("Active Spring Profiles: {}", String.join(", ", activeProfiles.length > 0 ? activeProfiles : new String[]{"default"}));
        log.info("Graylog Host: {}", graylogHost);
        log.info("Graylog Port: {}", graylogPort);
        log.info("Graylog Enabled: {}", java.util.Arrays.asList(activeProfiles).contains("graylog-enabled"));
        log.info("Test log message to verify Graylog integration");
        log.info("==========================================");
    }
}

