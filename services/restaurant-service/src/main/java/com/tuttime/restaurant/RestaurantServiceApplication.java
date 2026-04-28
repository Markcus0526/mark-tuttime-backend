package com.tuttime.restaurant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.data.redis.RedisHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
		scanBasePackages = "com.tuttime",
		exclude = {
				RedisAutoConfiguration.class,
				RedisRepositoriesAutoConfiguration.class,
				RedisReactiveHealthContributorAutoConfiguration.class,
				RedisHealthContributorAutoConfiguration.class
		}
)
@EnableScheduling
@Slf4j
public class RestaurantServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestaurantServiceApplication.class, args);
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
