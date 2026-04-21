package com.tuttime.review.grpc.client;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

		BranchValidResponse response = restaurantServiceBlockingStub.validateBranch(request);

		if (!response.getActive() || !response.getExists()) {
			throw new InvalidRequestException("You do not manage this specific branch");
		}
	}
}