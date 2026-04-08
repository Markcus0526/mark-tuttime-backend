package com.tuttime.gateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Rate Limiting Configuration: Bucket4j with Redis backend for distributed rate limiting. IP-based for unauthenticated, user-based for authenticated endpoints.
 */
@Slf4j
@Configuration
public class RateLimitingConfig {

	@Value("${spring.data.redis.sentinel.master:}")
	private String sentinelMaster;

	@Value("${spring.data.redis.sentinel.nodes:}")
	private String sentinelNodes;

	@Value("${spring.data.redis.host:tuttime-redis}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	@Value("${spring.data.redis.password:}")
	private String redisPassword;

	// --- Updated Property Keys (Removed brackets/quotes) ---

	@Value("${rate.limit.signin.capacity:5}")
	private int signinCapacity;

	@Value("${rate.limit.signin.refill.period.minutes:1}")
	private int signinRefillPeriodMinutes;

	@Value("${rate.limit.signup.capacity:3}")
	private int signupCapacity;

	@Value("${rate.limit.signup.refill.period.minutes:1}")
	private int signupRefillPeriodMinutes;

	@Value("${rate.limit.signup.sendsms.capacity:3}")
	private int signupSendSmsCapacity;

	@Value("${rate.limit.signup.sendsms.refill.period.hours:1}")
	private int signupSendSmsRefillPeriodHours;

	@Value("${rate.limit.signup.verifysms.capacity:10}")
	private int signupVerifySmsCapacity;

	@Value("${rate.limit.signup.verifysms.refill.period.hours:1}")
	private int signupVerifySmsRefillPeriodHours;

	@Value("${rate.limit.password.forgot.capacity:3}")
	private int passwordForgotCapacity;

	@Value("${rate.limit.password.forgot.refill.period.hours:1}")
	private int passwordForgotRefillPeriodHours;

	@Value("${rate.limit.password.verifysms.capacity:10}")
	private int passwordVerifySmsCapacity;

	@Value("${rate.limit.password.verifysms.refill.period.hours:1}")
	private int passwordVerifySmsRefillPeriodHours;

	@Value("${rate.limit.auth.refresh.capacity:10}")
	private int authRefreshCapacity;

	@Value("${rate.limit.auth.refresh.refill.period.minutes:1}")
	private int authRefreshRefillPeriodMinutes;

	@Value("${rate.limit.authenticated.capacity:500}")
	private int authenticatedCapacity;

	@Value("${rate.limit.authenticated.refill.period.minutes:1}")
	private int authenticatedRefillPeriodMinutes;

	/**
	 * Creates Redis client for Bucket4j rate limiting (blocking operations, separate from reactive Redis).
	 *
	 * <p>Connection mode selection:
	 * - Kubernetes: Sentinel is configured (SPRING_DATA_REDIS_SENTINEL_MASTER and SPRING_DATA_REDIS_SENTINEL_NODES are set)
	 * → Uses Redis Sentinel for high availability (no direct host connection)
	 * - Docker/Local: Sentinel is NOT configured (both master and nodes are empty)
	 * → Uses direct Redis connection (host:port)
	 * This ensures proper Redis connection strategy for each environment.
	 */
	@Bean
	public RedisClient redisClient() {
		RedisURI.Builder uriBuilder;

		// STRICT CHECK: Must be non-null AND have actual text
		boolean useSentinel = org.springframework.util.StringUtils.hasText(sentinelMaster) &&
				org.springframework.util.StringUtils.hasText(sentinelNodes);


		if (useSentinel) {
			uriBuilder = RedisURI.builder().withSentinelMasterId(sentinelMaster.trim());
			for (String node : sentinelNodes.split(",")) {
				String[] hostPort = node.trim().split(":");
				if (hostPort.length == 2) {
					uriBuilder.withSentinel(hostPort[0], Integer.parseInt(hostPort[1]));
				}
			}
		} else {
			// Force Standalone for Docker/Local
			uriBuilder = RedisURI.builder()
					.withHost(redisHost)
					.withPort(redisPort);
		}

		if (org.springframework.util.StringUtils.hasText(redisPassword)) {
			uriBuilder.withPassword(redisPassword.toCharArray());
		}

		return RedisClient.create(uriBuilder.build());
	}


	/**
	 * Creates Redis connection for Bucket4j proxy manager
	 */
	@Bean
	public StatefulRedisConnection<byte[], byte[]> redisConnection(RedisClient redisClient) {
		// STRICT CHECK: Must be non-null AND have actual text
		boolean useSentinel = org.springframework.util.StringUtils.hasText(sentinelMaster) &&
				org.springframework.util.StringUtils.hasText(sentinelNodes);

		try {
			StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(ByteArrayCodec.INSTANCE);
			// Connection success logging removed to reduce log noise - only errors will be logged
			return connection;
		} catch (Exception e) {
			log.error("Failed to connect to Redis for rate limiting: {}", e.getMessage());
			if (useSentinel) {
				log.error("Please ensure Redis Sentinel is running and accessible. Master: {}, Nodes: {}",
						sentinelMaster, sentinelNodes);
				throw new RuntimeException("Failed to connect to Redis Sentinel for rate limiting. Please check Redis Sentinel configuration and ensure it is running.", e);
			} else {
				log.error("Please ensure Redis is running and accessible. Host: {}, Port: {}",
						redisHost, redisPort);
				throw new RuntimeException("Failed to connect to Redis for rate limiting. Please check Redis configuration and ensure it is running.", e);
			}
		}
	}

	/**
	 * Creates Bucket4j proxy manager for distributed rate limiting (stores buckets in Redis).
	 */
	@Bean
	public LettuceBasedProxyManager<byte[]> proxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
		LettuceBasedProxyManager<byte[]> proxyManager = LettuceBasedProxyManager.builderFor(connection)
				.withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(24)))
				.build();

		// Proxy manager creation logging removed to reduce log noise
		return proxyManager;
	}

	/**
	 * Rate limit configuration map: endpoint path pattern -> rate limit supplier.
	 */
	@Bean
	public Map<String, Supplier<Bucket>> rateLimitConfigurations(LettuceBasedProxyManager<byte[]> proxyManager) {
		Map<String, Supplier<Bucket>> configurations = new HashMap<>();

		// /api/v1/auth/signin: Configurable requests per IP per time window
		int signinCapacityValue = signinCapacity;
		int signinRefillPeriodMinutesValue = signinRefillPeriodMinutes;
		configurations.put("/api/v1/auth/register", () -> {
			Bandwidth limit = Bandwidth.builder()
					.capacity(signinCapacityValue)
					.refillIntervally(signinCapacityValue, Duration.ofMinutes(signinRefillPeriodMinutesValue))
					.build();
			BucketConfiguration bucketConfig = BucketConfiguration.builder()
					.addLimit(limit)
					.build();
			return proxyManager.builder()
					.build("rate-limit:signin:".getBytes(), () -> bucketConfig);
		});

		// /api/v1/auth/signup: Configurable requests per IP per time window
		int signupCapacityValue = signupCapacity;
		int signupRefillPeriodMinutesValue = signupRefillPeriodMinutes;
		configurations.put("/api/v1/auth/register", () -> {
			Bandwidth limit = Bandwidth.builder()
					.capacity(signupCapacityValue)
					.refillIntervally(signupCapacityValue, Duration.ofMinutes(signupRefillPeriodMinutesValue))
					.build();
			BucketConfiguration bucketConfig = BucketConfiguration.builder()
					.addLimit(limit)
					.build();
			return proxyManager.builder()
					.build("rate-limit:signup:".getBytes(), () -> bucketConfig);
		});

		// /api/v1/auth/signup/send-sms: Configurable requests per IP per time window
		int signupSendSmsCapacityValue = signupSendSmsCapacity;
		int signupSendSmsRefillPeriodHoursValue = signupSendSmsRefillPeriodHours;
		configurations.put("/api/v1/auth/signup/send-sms", () -> {
			Bandwidth limit = Bandwidth.builder()
					.capacity(signupSendSmsCapacityValue)
					.refillIntervally(signupSendSmsCapacityValue, Duration.ofHours(signupSendSmsRefillPeriodHoursValue))
					.build();
			BucketConfiguration bucketConfig = BucketConfiguration.builder()
					.addLimit(limit)
					.build();
			return proxyManager.builder()
					.build("rate-limit:signup-send-sms:".getBytes(), () -> bucketConfig);
		});

		// /api/v1/auth/signup/verify-sms: Configurable requests per IP per time window
		int signupVerifySmsCapacityValue = signupVerifySmsCapacity;
		int signupVerifySmsRefillPeriodHoursValue = signupVerifySmsRefillPeriodHours;
		configurations.put("/api/v1/auth/signup/verify-sms", () -> {
			Bandwidth limit = Bandwidth.builder()
					.capacity(signupVerifySmsCapacityValue)
					.refillIntervally(signupVerifySmsCapacityValue, Duration.ofHours(signupVerifySmsRefillPeriodHoursValue))
					.build();
			BucketConfiguration bucketConfig = BucketConfiguration.builder()
					.addLimit(limit)
					.build();
			return proxyManager.builder()
					.build("rate-limit:signup-verify-sms:".getBytes(), () -> bucketConfig);
		});

		// /api/v1/password/forgot: Configurable requests per IP per time window
		int passwordForgotCapacityValue = passwordForgotCapacity;
		int passwordForgotRefillPeriodHoursValue = passwordForgotRefillPeriodHours;
		configurations.put("/api/v1/password/forgot", () -> {
			Bandwidth limit = Bandwidth.builder()
					.capacity(passwordForgotCapacityValue)
					.refillIntervally(passwordForgotCapacityValue, Duration.ofHours(passwordForgotRefillPeriodHoursValue))
					.build();
			BucketConfiguration bucketConfig = BucketConfiguration.builder()
					.addLimit(limit)
					.build();
			return proxyManager.builder()
					.build("rate-limit:password-forgot:".getBytes(), () -> bucketConfig);
		});

		// /api/v1/password/verify-sms: Configurable requests per IP per time window
		int passwordVerifySmsCapacityValue = passwordVerifySmsCapacity;
		int passwordVerifySmsRefillPeriodHoursValue = passwordVerifySmsRefillPeriodHours;
		configurations.put("/api/v1/password/verify-sms", () -> {
			Bandwidth limit = Bandwidth.builder()
					.capacity(passwordVerifySmsCapacityValue)
					.refillIntervally(passwordVerifySmsCapacityValue, Duration.ofHours(passwordVerifySmsRefillPeriodHoursValue))
					.build();
			BucketConfiguration bucketConfig = BucketConfiguration.builder()
					.addLimit(limit)
					.build();
			return proxyManager.builder()
					.build("rate-limit:password-verify-sms:".getBytes(), () -> bucketConfig);
		});

		// /api/v1/auth/refresh: Configurable requests per user per time window
		int authRefreshCapacityValue = authRefreshCapacity;
		int authRefreshRefillPeriodMinutesValue = authRefreshRefillPeriodMinutes;
		configurations.put("/api/v1/auth/refresh", () -> {
			Bandwidth limit = Bandwidth.builder()
					.capacity(authRefreshCapacityValue)
					.refillIntervally(authRefreshCapacityValue, Duration.ofMinutes(authRefreshRefillPeriodMinutesValue))
					.build();
			BucketConfiguration bucketConfig = BucketConfiguration.builder()
					.addLimit(limit)
					.build();
			return proxyManager.builder()
					.build("rate-limit:refresh:".getBytes(), () -> bucketConfig);
		});

		// Default for authenticated endpoints: Configurable requests per user per time window
		int authenticatedCapacityValue = authenticatedCapacity;
		int authenticatedRefillPeriodMinutesValue = authenticatedRefillPeriodMinutes;
		configurations.put("authenticated", () -> {
			Bandwidth limit = Bandwidth.builder()
					.capacity(authenticatedCapacityValue)
					.refillIntervally(authenticatedCapacityValue, Duration.ofMinutes(authenticatedRefillPeriodMinutesValue))
					.build();
			BucketConfiguration bucketConfig = BucketConfiguration.builder()
					.addLimit(limit)
					.build();
			return proxyManager.builder()
					.build("rate-limit:authenticated:".getBytes(), () -> bucketConfig);
		});

		log.info("Configured {} rate limit configurations", configurations.size());
		return configurations;
	}

	/**
	 * Helper method to get rate limit configuration for a given path.
	 */
	public static Supplier<Bucket> getRateLimitForPath(String path, Map<String, Supplier<Bucket>> configurations) {
		if (configurations.containsKey(path)) {
			return configurations.get(path);
		}

		for (Map.Entry<String, Supplier<Bucket>> entry : configurations.entrySet()) {
			String pattern = entry.getKey();
			if (path.startsWith(pattern)) {
				return entry.getValue();
			}
		}

		return null;
	}

	/**
	 * Helper method to determine if a path requires authenticated rate limiting.
	 */
	public static boolean requiresAuthenticatedRateLimit(String path) {
		if (path.startsWith("/api/v1/auth/refresh")) {
			return true;
		}

		String[] publicPaths = {
				"/api/v1/auth/signin",
				"/api/v1/auth/signup",
				"/api/v1/password/forgot",
				"/api/v1/users/register",
				"/api/v1/users/login",
				"/actuator"
		};

		for (String publicPath : publicPaths) {
			if (path.startsWith(publicPath)) {
				return false;
			}
		}

		// If path starts with /api/v1, it's likely an authenticated endpoint
		return path.startsWith("/api/v1/");
	}
}

