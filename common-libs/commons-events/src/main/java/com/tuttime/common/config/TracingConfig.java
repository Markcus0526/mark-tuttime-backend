package com.tuttime.common.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Distributed Tracing Configuration
 * Uses Micrometer Tracing with Zipkin/Brave
 */
@Slf4j
@Configuration
@ConditionalOnClass(Tracer.class)
public class TracingConfig {

    /**
     * Example of using tracing in services
     *
     * @NewSpan("operation-name")
     * public void method(@SpanTag("param") String param) {
     *     // Method implementation
     * }
     */
}

