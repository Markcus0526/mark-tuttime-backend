package com.tuttime.review.grpc.client;

import com.tuttime.common.proto.UserByIdRequest;
import com.tuttime.common.proto.UserServiceGrpc;
import com.tuttime.common.proto.UserValidResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userStub;

	public boolean isUserValid(UUID userId) {
		UserByIdRequest request = UserByIdRequest.newBuilder()
				.setUserId(userId.toString())
				.build();

		UserValidResponse response = userStub.validateUser(request);
		return response.getIsValid();
	}
}
