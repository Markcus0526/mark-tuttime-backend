package com.tuttime.user.warmup;

import com.tuttime.common.warmup.BaseApplicationWarmup;
import com.tuttime.user.grpc.client.RestaurantGrpcClient;
import com.tuttime.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserWarmup extends BaseApplicationWarmup {
	private final UserRepository userRepository;
	private final StringRedisTemplate redisTemplate;
	private final RestaurantGrpcClient restaurantClient;

	@EventListener(ApplicationReadyEvent.class)
	public void warmUp() {
		log.info("Warming up Booking Service...");
		warmUpDatabase(userRepository);
		warmUpRedis(redisTemplate);
//		warmUpGrpc(() -> restaurantClient.validateBranchPermission(NIL_UUID, NIL_UUID, "ADMIN")); TODO
	}
}