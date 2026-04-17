package com.tuttime.restaurant.grpc.client;

import com.tuttime.common.proto.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

	public UserValidResponse validateUser(UserValidRequest request) {
		return userServiceBlockingStub.validateUser(request);
	}

	public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
		return userServiceBlockingStub.getUserDetails(request);
	}
}
