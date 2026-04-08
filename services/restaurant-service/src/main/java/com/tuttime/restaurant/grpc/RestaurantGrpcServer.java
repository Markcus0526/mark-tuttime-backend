package com.tuttime.restaurant.grpc;

import com.tuttime.common.proto.*;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.TableRepository;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@GrpcService // Annotation from net.devh:grpc-server-spring-boot-starter
@RequiredArgsConstructor
public class RestaurantGrpcServer extends RestaurantServiceGrpc.RestaurantServiceImplBase {

	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final TableRepository tableRepository;

	@PostConstruct
	public void init() {
		log.info("gRPC: RestaurantGrpcProvider initialized");
	}

	@Override
	public void validateBranch(BranchRequest request, StreamObserver<BranchResponse> responseObserver) {
		try {
			UUID branchId = UUID.fromString(request.getBranchId());
			UUID userId = UUID.fromString(request.getUserId());

			boolean exists = branchRepository.findByBranchIdAndIsDeletedFalse(branchId)
					.map(branch ->
							branch.getManagerUserId().equals(userId) ||
									restaurantRepository.existsByRestaurantIdAndOwnerUserIdAndIsDeletedFalse(
											branch.getRestaurantId(), userId)
					).orElse(false);

			responseObserver.onNext(BranchResponse.newBuilder().setExists(exists).build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("Validation failed for branch: {}", request.getBranchId(), e);

			responseObserver.onNext(BranchResponse.newBuilder().setExists(false).build());
			responseObserver.onCompleted();
		}
	}


	@Override
	public void checkAvailableTableForBooking(AvailableTableRequest request, StreamObserver<AvailableTableResponse> responseObserver) {
		UUID tableId = UUID.fromString(request.getTableId());
		int partSize = request.getPartySize();

		boolean available = tableRepository.findByTableIdAndIsDeletedFalse(tableId)
				.map(table ->
						table.getStatus() == TableStatus.ACTIVE && table.getSeats() <= partSize
				).orElse(false);

		responseObserver.onNext(AvailableTableResponse.newBuilder().setIsAvailable(available).build());
		responseObserver.onCompleted();
	}
}