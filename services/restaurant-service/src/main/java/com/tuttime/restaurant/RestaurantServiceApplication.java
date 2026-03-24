package com.tuttime.restaurant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.tuttime.restaurant", "com.tuttime.common"})
//@EnableDiscoveryClient
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
@EnableScheduling
@EntityScan("com.tuttime.restaurant.entity")
@EnableJpaRepositories("com.tuttime.restaurant.repository")
@Slf4j
public class RestaurantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantServiceApplication.class, args);
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
