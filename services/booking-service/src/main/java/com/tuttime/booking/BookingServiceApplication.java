package com.tuttime.booking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication(scanBasePackages = "com.tuttime")
@EnableScheduling
public class BookingServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
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
