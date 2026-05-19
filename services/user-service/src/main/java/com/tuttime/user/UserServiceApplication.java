package com.tuttime.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * User Service Application Entry Point
 *
 * <p>Main Spring Boot application class for the User Service microservice.
 * Handles user registration, authentication, profile management, and KYC processing.
 */
@SpringBootApplication(
        scanBasePackages = "com.tuttime",
        exclude = CacheAutoConfiguration.class
)
@EnableScheduling
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

