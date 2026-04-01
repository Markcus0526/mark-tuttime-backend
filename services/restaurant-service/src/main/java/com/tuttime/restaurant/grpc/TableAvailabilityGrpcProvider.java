package com.tuttime.restaurant.grpc;

import com.tuttime.common.proto.AvailabilityRequest;
import com.tuttime.common.proto.AvailabilityResponse;
import com.tuttime.common.proto.RestaurantServiceGrpc;
import com.tuttime.common.proto.TableInfo;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.repository.TableRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@GrpcService // Annotation from net.devh:grpc-server-spring-boot-starter
@RequiredArgsConstructor
public class TableAvailabilityGrpcProvider extends RestaurantServiceGrpc.RestaurantServiceImplBase {

	private final TableRepository tableRepository;

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