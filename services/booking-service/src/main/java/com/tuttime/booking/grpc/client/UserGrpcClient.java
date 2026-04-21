package com.tuttime.booking.grpc.client;

import com.tuttime.common.proto.UserDetailRequest;
import com.tuttime.common.proto.UserDetailResponse;
import com.tuttime.common.proto.UserServiceGrpc;
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

	public UserDetailResponse getUserDetail(UserDetailRequest request) {
		return userServiceBlockingStub.getUserDetail(request);
	}
}