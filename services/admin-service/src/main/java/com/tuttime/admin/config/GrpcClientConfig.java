package com.tuttime.admin.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * gRPC Client Configuration
 *
 * <p>Provides utility methods for creating gRPC managed channels with optimized settings.
 * Per-service client configuration is handled by spring-boot-starter-grpc via application.yml.
 * Critical for 10M user scale to prevent hanging requests.
 */
@Slf4j
@Configuration
public class GrpcClientConfig {

	/**
	 * Create managed channel with optimized settings for high load
	 *
	 * <p>Note: This is a utility method. Actual gRPC clients are configured
	 * via @GrpcClient annotation and application.yml per-service settings.
	 */
	public static ManagedChannel createManagedChannel(String target) {
		return ManagedChannelBuilder.forTarget(target)
				.usePlaintext() // Use TLS in production
				.maxInboundMessageSize(4 * 1024 * 1024) // 4MB max message size
				.keepAliveTime(30, TimeUnit.SECONDS)
				.keepAliveTimeout(5, TimeUnit.SECONDS)
				.keepAliveWithoutCalls(true)
				.maxRetryAttempts(3)
				.build();
	}
}

