package com.tuttime.order.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.UserDetailRequest;
import com.tuttime.common.event.proto.UserDetailResponse;
import com.tuttime.common.event.proto.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.USER_SERVICE;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

	public UserDetailResponse getUserDetail(UserDetailRequest request) {
		return GrpcExecutor.execute(USER_SERVICE, "getUserDetail", () ->
				userServiceBlockingStub.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getUserDetail(request));
	}
}