package com.tuttime.restaurant.grpc;

import com.tuttime.common.proto.*;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.RestaurantStaffRepository;
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

	private final RestaurantStaffRepository staffRepository;
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

			boolean exists = staffRepository.existsByBranchIdAndUserId(branchId, userId);

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

	@Override
	public void getTablesForBooking(SlotAvailabilityRequest request, StreamObserver<TableListResponse> responseObserver) {
		UUID branchId = UUID.fromString(request.getBranchId());
		int partySize = request.getPartySize();

		// 1. Fetch tables from SQL that are active and can fit the party size
		List<Object[]> results = tableRepository.findTablesWithZoneName(branchId, partySize);

		// 2. Build the Protobuf response
		TableListResponse.Builder responseBuilder = TableListResponse.newBuilder();

		for (Object[] row : results) {
			TableInfo info = TableInfo.newBuilder()
					.setTableId(row[0].toString())
					.setTableNumber(row[1].toString())
					.setZoneName(row[3].toString()) // Accesses the Zone Name directly from the query
					.setMaxCapacity((Integer) row[2])
					.build();
			responseBuilder.addTables(info);
		}

		// 3. Send back to the Booking Service
		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}
}