package com.tuttime.menu.grpc.client;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.BranchRequest;
import com.tuttime.common.proto.BranchResponse;
import com.tuttime.common.proto.RestaurantServiceGrpc;
import com.tuttime.common.proto.UserServiceGrpc;
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

	public void checkBranchExists(UUID userId, UUID branchId) {
		BranchRequest request = BranchRequest.newBuilder()
				.setUserId(userId.toString())
				.setBranchId(branchId.toString())
				.build();

		BranchResponse response = restaurantServiceBlockingStub.validateBranch(request);

		if (!response.getExists()) {
			throw new ConflictException("You do not manage this specific menu");
		}
	}
}