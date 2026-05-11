package com.tuttime.user.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.RestaurantServiceGrpc;
import com.tuttime.common.event.proto.StaffUserRequest;
import com.tuttime.common.event.proto.StaffUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.RESTAURANT_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("restaurant-service")
	private RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantServiceBlockingStub;

	private final GrpcExecutor grpcExecutor;

	public StaffUserResponse getStaffForUser(StaffUserRequest request) {
		return grpcExecutor.execute(RESTAURANT_SERVICE, "getStaffForUser", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getStaffForUser(request));
	}
}