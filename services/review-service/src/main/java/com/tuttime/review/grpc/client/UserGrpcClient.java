package com.tuttime.review.grpc.client;

import com.tuttime.common.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

	public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
		return userServiceBlockingStub.getUserDetails(request);
	}
}