package com.tuttime.booking.grpc.client;

import com.tuttime.common.proto.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userStub;

	/**
	 * Validates the Access Token and returns the User Context.
	 * Used by Security Filters.
	 */
	public UserContextResponse validateToken(String token) {
		try {
			ValidateUserRequest request = ValidateUserRequest.newBuilder()
					.setJwtToken(token)
					.build();
			return userStub.validateUser(request, new StreamObserver<UserContextResponse>() {
				@Override
				public void onNext(UserContextResponse value) {
					sink.success(value);
				}

				@Override
				public void onError(Throwable t) {                    // Propagate gRPC error to the Mono stream 					sink.error(t); 				}  				@Override 				public void onCompleted() { 					// No action needed for Unary calls, onNext already triggered success 				} 			}
			);
		} catch (StatusRuntimeException e) {
			log.error("gRPC Auth failed: {}", e.getStatus().getDescription());
			throw e; // Let the global exception handler handle it
		}
	}

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