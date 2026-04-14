package com.tuttime.menu.grpc.client;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.BranchPermissionRequest;
import com.tuttime.common.proto.BranchPermissionResponse;
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

	public void validateBranchPermission(UUID branchId, UUID userId, String userRoles) {
		BranchPermissionRequest request = BranchPermissionRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setUserId(userId.toString())
				.setUserRoles(userRoles)
				.build();

		BranchPermissionResponse response = restaurantServiceBlockingStub.validateBranchPermission(request);

		if (!response.getValid()) {
			throw new MethodArgumentNotValidException("You do not manage this specific menu");
		}
	}
}