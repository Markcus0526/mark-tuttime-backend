package com.tuttime.review.grpc.client;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.BranchValidRequest;
import com.tuttime.common.event.proto.BranchValidResponse;
import com.tuttime.common.event.proto.RestaurantServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.RESTAURANT_SERVICE;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantGrpcClient {

	@GrpcClient("restaurant-service")
	private RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantServiceBlockingStub;

	public void validateBranch(UUID branchId) {
		BranchValidRequest request = BranchValidRequest.newBuilder()
				.setBranchId(branchId.toString())
				.build();

		BranchValidResponse response = GrpcExecutor.execute(RESTAURANT_SERVICE, "validateBranch", () ->
				restaurantServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.validateBranch(request));

		if (!response.getActive() || !response.getExists()) {
			throw new InvalidRequestException("You do not manage this specific branch");
		}
	}
}