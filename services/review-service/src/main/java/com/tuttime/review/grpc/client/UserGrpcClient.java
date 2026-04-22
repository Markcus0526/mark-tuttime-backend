package com.tuttime.review.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.UserDetailsRequest;
import com.tuttime.common.event.proto.UserDetailsResponse;
import com.tuttime.common.event.proto.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.USER_SERVICE;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

	public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
		return GrpcExecutor.execute(USER_SERVICE, "getUserDetails", () ->
				userServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getUserDetails(request));
	}
}