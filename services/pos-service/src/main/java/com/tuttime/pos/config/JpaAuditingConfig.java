package com.tuttime.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;


@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    /**
     * DateTimeProvider that returns OffsetDateTime in UTC.
     * This allows Spring Data JPA auditing to work with OffsetDateTime fields.
     *
     * @return DateTimeProvider that provides OffsetDateTime instances
     */
    public JpaAuditingConfig() {
//		System.out.println("DEBUG: JpaAuditingConfig has been loaded!");
    }

    @Bean(name = "offsetDateTimeProvider")
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }
}

