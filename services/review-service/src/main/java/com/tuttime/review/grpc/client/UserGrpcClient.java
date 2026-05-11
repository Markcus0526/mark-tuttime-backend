package com.tuttime.review.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.UserDetailsRequest;
import com.tuttime.common.event.proto.UserDetailsResponse;
import com.tuttime.common.event.proto.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.USER_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

	private final GrpcExecutor grpcExecutor;

	public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
		return grpcExecutor.execute(USER_SERVICE, "getUserDetails", () ->
				userServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getUserDetails(request));
	}
}