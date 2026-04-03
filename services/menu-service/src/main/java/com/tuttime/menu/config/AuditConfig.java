package com.tuttime.menu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider") // Defined here
public class AuditConfig {

	@Bean
	public DateTimeProvider offsetDateTimeProvider() { // Referenced here
		return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
	}
}