package com.tuttime.review.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.BranchValidRequest;
import com.tuttime.common.event.proto.BranchValidResponse;
import com.tuttime.common.event.proto.RestaurantServiceGrpc;
import com.tuttime.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
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

	public void validateBranch(UUID branchId) {
		BranchValidRequest request = BranchValidRequest.newBuilder()
				.setBranchId(branchId.toString())
				.build();

		BranchValidResponse response = grpcExecutor.execute(RESTAURANT_SERVICE, "validateBranch", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.validateBranch(request));

		if (!response.getActive() || !response.getExists()) {
			throw new InvalidRequestException("You do not manage this specific branch");
		}
	}
}