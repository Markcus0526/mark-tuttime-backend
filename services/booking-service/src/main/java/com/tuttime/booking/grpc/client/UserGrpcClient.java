package com.tuttime.booking.grpc.client;

import com.tuttime.common.proto.*;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;


	/**
	 * Fetches full profile details based on your SQL schema.
	 * Used for Booking confirmation details.
	 */
	public UserProfileResponse getProfile(String userId) {
		try {
			UserByIdRequest request = UserByIdRequest.newBuilder()
					.setUserId(userId)
					.build();
			return userServiceBlockingStub.getUserProfile(request);
		} catch (StatusRuntimeException e) {
			log.error("Failed to fetch profile for user {}: {}", userId, e.getStatus());
			return null;
		}
	}
}