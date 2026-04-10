package com.tuttime.restaurant.grpc.client;

import com.tuttime.common.proto.UserByIdRequest;
import com.tuttime.common.proto.UserProfileResponse;
import com.tuttime.common.proto.UserServiceGrpc;
import com.tuttime.common.proto.UserValidResponse;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userBockingStub;

	public UserValidResponse validateUserById(UUID userId) {
		try {
			UserByIdRequest request = UserByIdRequest.newBuilder()
					.setUserId(userId.toString())
					.build();
			return userBockingStub.validateUserById(request);
		} catch (StatusRuntimeException e) {
			log.error("Failed to fetch validation for user {}: {}", userId, e.getStatus());
			return null;
		}
	}
}
