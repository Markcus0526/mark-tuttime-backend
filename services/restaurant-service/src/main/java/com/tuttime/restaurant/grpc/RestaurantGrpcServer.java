package com.tuttime.restaurant.grpc;

import com.tuttime.common.proto.*;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Table;
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
	public void checkAvailability(AvailabilityRequest request, StreamObserver<AvailabilityResponse> responseObserver) {
		// 1. Convert Protobuf Timestamp to Java Instant
		Instant start = Instant.ofEpochSecond(request.getStartTime().getSeconds(), request.getStartTime().getNanos());
		Instant end = start.plus(Duration.ofMinutes(request.getDurationMinutes()));
		UUID branchId = UUID.fromString(request.getBranchId());

		// 2. Query DB (Using the native overlap check logic we built earlier)
		List<Table> freeTables = tableRepository.findAvailableTablesInBranch(
				branchId, start, end, request.getPartySize()
		);

		// 3. Build Response
		var responseBuilder = AvailabilityResponse.newBuilder()
				.setIsAvailable(!freeTables.isEmpty());

		freeTables.forEach(t -> responseBuilder.addAvailableTables(
				TableInfo.newBuilder()
						.setTableId(t.getTableId().toString())
						.setZoneId(t.getZoneId().toString())
						.setSeats(t.getSeats())
						.build()
		));

		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}
}