package com.tuttime.gateway.grpc.client;

import com.tuttime.common.proto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userBockingStub;

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceStub userAsyncStub;

	/**
	 * Validates the Access Token and returns the User Context.
	 * Used by Security Filters.
	 */
	@CircuitBreaker(name = "userService", fallbackMethod = "validateFallback")
	public Mono<UserContextResponse> validateUserToken(String token) {
		return Mono.create(sink -> {
			ValidateUserRequest request = ValidateUserRequest.newBuilder()
					.setJwtToken(token)
					.build();

			userAsyncStub.validateUserToken(request, new StreamObserver<UserContextResponse>() {
				@Override
				public void onNext(UserContextResponse value) {
					sink.success(value);
				}

				@Override
				public void onError(Throwable t) {
					// Propagate gRPC error to the Mono stream
					sink.error(t);
				}

				@Override
				public void onCompleted() {
					// No action needed for Unary calls, onNext already triggered success
				}
			});
		});
	}

	/**
	 * Fetches full profile details based on your SQL schema.
	 * Used for Booking confirmation details.
	 */
	public UserProfileResponse getProfile(String userId) {
		try {
			UserByIdRequest request = UserByIdRequest.newBuilder()
					.setUserId(userId)
					.build();
			return userBockingStub.getUserProfile(request);
		} catch (StatusRuntimeException e) {
			log.error("Failed to fetch profile for user {}: {}", userId, e.getStatus());
			return null;
		}
	}

	/**
	 * Fallback logic: If User Service is down, we allow the request
	 * but log a critical warning (or reject depending on your security policy).
	 */
	private Mono<UserContextResponse> validateFallback(String token, Throwable t) {
		log.error("User Service gRPC failed (Circuit Open). Reason: {}", t.getMessage());
		// Option A: Return a dummy context to allow operation (if token is valid)
		// Option B: Mono.error(new SecurityException("Auth Service Unavailable"));
		return Mono.error(new RuntimeException("Identity service is currently unavailable"));
	}
}
