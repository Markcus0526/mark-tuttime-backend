package com.tuttime.booking.grpc.client;

import com.tuttime.common.proto.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userStub;


	/**
	 * Fetches full profile details based on your SQL schema.
	 * Used for Booking confirmation details.
	 */
	public UserProfileResponse getProfile(String userId) {
		try {
			GetUserRequest request = GetUserRequest.newBuilder()
					.setUserId(userId)
					.build();
			return userStub.getUserProfile(request);
		} catch (StatusRuntimeException e) {
			log.error("Failed to fetch profile for user {}: {}", userId, e.getStatus());
			return null;
		}
	}
}