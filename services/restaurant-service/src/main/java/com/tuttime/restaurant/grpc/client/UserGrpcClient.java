package com.tuttime.restaurant.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.USER_SERVICE;

@Service
@Slf4j
public class UserGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

	public UserValidResponse validateUser(UserValidRequest request) {
		return GrpcExecutor.execute(USER_SERVICE, "validateUser", () ->
				userServiceBlockingStub.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.validateUser(request));
	}

	public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
		return GrpcExecutor.execute(USER_SERVICE, "getUserDetails", () ->
				userServiceBlockingStub.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getUserDetails(request));
	}
}
