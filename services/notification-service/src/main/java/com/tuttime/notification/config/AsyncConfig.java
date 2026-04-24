package com.tuttime.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "notificationExecutor")
	public Executor notificationExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);      // 10 threads always running
		executor.setMaxPoolSize(50);       // Can scale up to 50 threads
		executor.setQueueCapacity(10000);  // Store up to 10k messages in memory before rejecting
		executor.setThreadNamePrefix("NotifAsync-");

		// Critical: When the queue is full, the calling thread (Kafka Consumer)
		// will handle the task itself to prevent data loss.
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		executor.initialize();
		return executor;
	}

}