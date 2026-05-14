package com.tuttime.booking.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
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

	public void validateBranchPermission(UUID branchId, UUID userId, String userRoles) {
		ValidateBranchPermissionRequest request = ValidateBranchPermissionRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setUserId(userId.toString())
				.setUserRoles(userRoles)
				.build();

		boolean valid = grpcExecutor.execute(RESTAURANT_SERVICE, "validateBranchPermission", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.validateBranchPermission(request).getValid());

		if (!valid) {
			throw new InvalidRequestException("You do not manage this specific branch");
		}
	}

	public GetTableDetailResponse getTableDetail(UUID tableId) {
		GetTableDetailRequest request = GetTableDetailRequest.newBuilder()
				.setTableId(tableId.toString())
				.build();

		return grpcExecutor.execute(RESTAURANT_SERVICE, "getTableDetail", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getTableDetail(request));
	}

	public List<TableSlotPto> getBranchSlots(UUID branchId, int partySize) {
		GetBranchSlotsRequest request = GetBranchSlotsRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setPartySize(partySize)
				.build();

		return grpcExecutor.execute(RESTAURANT_SERVICE, "getBranchSlots", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getBranchSlots(request).getTablesList());
	}

	public GetBranchScheduleResponse getBranchSchedule(UUID branchId, int dayOfWeek) {
		GetBranchScheduleRequest request = GetBranchScheduleRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setDayOfWeek(dayOfWeek)
				.build();

		return grpcExecutor.execute(RESTAURANT_SERVICE, "getBranchSchedule", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getBranchSchedule(request));
	}

	public GetBranchDetailResponse getBranchDetail(UUID branchId) {
		GetBranchDetailRequest request = GetBranchDetailRequest.newBuilder()
				.setBranchId(branchId.toString())
				.build();

		return grpcExecutor.execute(RESTAURANT_SERVICE, "getBranchDetail", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getBranchDetail(request));
	}

	public GetTableHierarchyDetailResponse getTableHierarchyDetail(GetTableHierarchyDetailRequest request) {
		return grpcExecutor.execute(RESTAURANT_SERVICE, "getTableHierarchyDetail", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getTableHierarchyDetail(request));
	}

	public FindAvailableTableResponse findAvailableTable(UUID branchId, UUID zoneId, int partySize, OffsetDateTime startTime) {
		FindAvailableTableRequest.Builder requestBuilder = FindAvailableTableRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setPartySize(partySize)
				.setStartTime(startTime.toString());
		if (zoneId != null) {
			requestBuilder.setZoneId(zoneId.toString());
		}

		return grpcExecutor.execute(RESTAURANT_SERVICE, "findAvailableTable", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.findAvailableTable(requestBuilder.build())
		);
	}
}