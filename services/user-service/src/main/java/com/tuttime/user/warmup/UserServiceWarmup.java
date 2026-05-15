package com.tuttime.user.warmup;

import com.tuttime.common.warmup.BaseApplicationWarmup;
import com.tuttime.user.grpc.client.RestaurantGrpcClient;
import com.tuttime.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceWarmup extends BaseApplicationWarmup {

	private final RoleRepository roleRepository;
	private final UserAuthMethodRepository userAuthMethodRepository;
	private final UserDeviceRepository userDeviceRepository;
	private final UserLocationRepository userLocationRepository;
	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final RestaurantGrpcClient restaurantGrpcClient;
	// add other repos, redis, etc.

	@EventListener(ApplicationReadyEvent.class)
	public void warmup() {
		log.info("[UserService: Warmup] Starting warmup");

		// Database warmup
		warmUpDatabase(
				roleRepository,
				userAuthMethodRepository,
				userDeviceRepository,
				userLocationRepository,
				userRepository,
				userRoleRepository
		);

		// Redis warmup (if you have Redis in this service)
		// warmUpRedis(redisTemplate);

		// gRPC warmup – call a lightweight method, e.g., health check
		warmUpAsync(() -> restaurantGrpcClient.ping());

		// HTTP warmup (if any)
		// warmUpHttp(() -> someWebClient.get().uri("/health").retrieve().bodyToMono(Void.class).block());

		log.info("[UserService: Warmup] warmup completed");
	}
}