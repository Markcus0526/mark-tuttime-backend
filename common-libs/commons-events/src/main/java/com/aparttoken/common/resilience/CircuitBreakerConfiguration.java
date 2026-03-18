package com.aparttoken.common.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration
 * Prevents cascading failures in distributed systems
 */
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit after 50% failures
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
                .slidingWindowSize(10) // Last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls before evaluating
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public CircuitBreaker defaultCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("default");
    }

    @Bean
    public CircuitBreaker grpcCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig grpcConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(60)
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .build();

        return registry.circuitBreaker("grpc", grpcConfig);
    }
}

