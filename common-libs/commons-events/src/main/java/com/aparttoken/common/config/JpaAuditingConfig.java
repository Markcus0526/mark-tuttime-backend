package com.aparttoken.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Shared JPA Auditing Configuration
 * 
 * <p>Configures Spring Data JPA auditing to use OffsetDateTime instead of LocalDateTime.
 * This ensures compatibility with entities that use OffsetDateTime for @CreatedDate and @LastModifiedDate.
 * 
 * <p>This configuration is shared across all services via the commons-events module.
 * Services should import this configuration by including commons-events in their dependencies
 * and ensuring @EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider") is present 
 * on their main application class or a @Configuration class.
 * 
 * <p><b>Usage:</b>
 * <ul>
 *   <li>Add @EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider") to your application class</li>
 *   <li>Or create a @Configuration class with @EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")</li>
 *   <li>This bean will be automatically discovered via component scanning</li>
 * </ul>
 */
@Configuration
public class JpaAuditingConfig {

    /**
     * DateTimeProvider that returns OffsetDateTime in UTC.
     * This allows Spring Data JPA auditing to work with OffsetDateTime fields.
     * 
     * @return DateTimeProvider that provides OffsetDateTime instances
     */
    @Bean(name = "offsetDateTimeProvider")
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }
}

