package com.tuttime.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for background tasks.
 * Used for SMS delivery verification polling to avoid blocking HTTP threads.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("smsDeliveryExecutor")
    public Executor smsDeliveryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sms-delivery-");
        executor.initialize();
        return executor;
    }
}
