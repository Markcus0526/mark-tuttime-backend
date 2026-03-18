package com.tuttime.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration
 * 
 * <p>Configures async execution for long-running operations.
 * Critical for 10M user scale to prevent blocking main threads.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor for parallel service calls
     * Optimized for high concurrency with 10M users
     */
    @Bean(name = "serviceCallExecutor")
    public Executor serviceCallExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50); // Base thread pool size
        executor.setMaxPoolSize(200); // Max threads for peak load
        executor.setQueueCapacity(1000); // Queue size before rejection
        executor.setThreadNamePrefix("admin-service-call-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Executor for async operations (bulk operations, exports, etc.)
     */
    @Bean(name = "asyncOperationExecutor")
    public Executor asyncOperationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("admin-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}

