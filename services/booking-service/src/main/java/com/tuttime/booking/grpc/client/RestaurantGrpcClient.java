package com.tuttime.booking.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
		BranchPermissionRequest request = BranchPermissionRequest.newBuilder()
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

	public TableDetailResponse getTableDetail(UUID tableId) {
		TableDetailRequest request = TableDetailRequest.newBuilder()
				.setTableId(tableId.toString())
				.build();

		return grpcExecutor.execute(RESTAURANT_SERVICE, "getTableDetail", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getTableDetail(request));
	}

	public List<TableSlotResponse> getBranchSlots(UUID branchId, int partySize) {
		BranchSlotsRequest request = BranchSlotsRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setPartySize(partySize)
				.build();

		return grpcExecutor.execute(RESTAURANT_SERVICE, "getBranchSlots", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getBranchSlots(request).getTablesList());
	}

	public ScheduleResponse getBranchSchedule(UUID branchId, int dayOfWeek) {
		ScheduleRequest request = ScheduleRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setDayOfWeek(dayOfWeek)
				.build();

		return grpcExecutor.execute(RESTAURANT_SERVICE, "getBranchSchedule", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getBranchSchedule(request));
	}

	public BranchDetailResponse getBranchDetail(UUID branchId) {
		BranchDetailRequest request = BranchDetailRequest.newBuilder()
				.setBranchId(branchId.toString())
				.build();

		return grpcExecutor.execute(RESTAURANT_SERVICE, "getBranchDetail", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getBranchDetail(request));
	}

	public TableHierarchyResponse getTableHierarchyDetail(TableHierarchyRequest request) {
		return grpcExecutor.execute(RESTAURANT_SERVICE, "getTableHierarchyDetail", () ->
				restaurantServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getTableHierarchyDetail(request));
	}
}