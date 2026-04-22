package com.tuttime.user.grpc.client;

import com.tuttime.common.grpc.GrpcExecutor;
import com.tuttime.common.proto.RestaurantServiceGrpc;
import com.tuttime.common.proto.StaffUserRequest;
import com.tuttime.common.proto.StaffUserResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.RESTAURANT_SERVICE;

@Service
@Slf4j
public class RestaurantGrpcClient {

	@GrpcClient("restaurant-service")
	private RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantServiceBlockingStub;

	public StaffUserResponse getStaffForUser(StaffUserRequest request) {
		return GrpcExecutor.execute(RESTAURANT_SERVICE, "getStaffForUser", () ->
				restaurantServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getStaffForUser(request));
	}
}
