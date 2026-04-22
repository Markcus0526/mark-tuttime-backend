package com.tuttime.booking.grpc.client;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.grpc.GrpcExecutor;
import com.tuttime.common.proto.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.ORDER_SERVICE;
import static com.tuttime.common.constants.ServiceNames.RESTAURANT_SERVICE;

@Service
@Slf4j
public class RestaurantGrpcClient {

	@GrpcClient("restaurant-service")
	private RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantServiceBlockingStub;

	public void validateBranchPermission(UUID branchId, UUID userId, String userRoles) {
		BranchPermissionRequest request = BranchPermissionRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setUserId(userId.toString())
				.setUserRoles(userRoles)
				.build();

		boolean valid = GrpcExecutor.execute(RESTAURANT_SERVICE, "validateBranchPermission", () ->
				restaurantServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.validateBranchPermission(request).getValid());

		if (!valid) {
			throw new InvalidRequestException("You do not manage this specific branch");
		}
	}

	public TableDetailResponse getTableDetail(UUID tableId) {
		TableDetailRequest request = TableDetailRequest.newBuilder()
				.setTableId(tableId.toString())
				.build();

		return GrpcExecutor.execute(RESTAURANT_SERVICE, "getTableDetail", () ->
				restaurantServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getTableDetail(request));
	}

	public List<TableSlotResponse> getBranchSlots(UUID branchId, int partySize) {
		BranchSlotsRequest request = BranchSlotsRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setPartySize(partySize)
				.build();

		return GrpcExecutor.execute(RESTAURANT_SERVICE, "getBranchSlots", () ->
				restaurantServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getBranchSlots(request).getTablesList());
	}

	public ScheduleResponse getBranchSchedule(UUID branchId, int dayOfWeek) {
		ScheduleRequest request = ScheduleRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setDayOfWeek(dayOfWeek)
				.build();

		return GrpcExecutor.execute(RESTAURANT_SERVICE, "getBranchSchedule", () ->
				restaurantServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getBranchSchedule(request));
	}

	public BranchDetailResponse getBranchDetail(UUID branchId) {
		BranchDetailRequest request = BranchDetailRequest.newBuilder()
				.setBranchId(branchId.toString())
				.build();

		return GrpcExecutor.execute(RESTAURANT_SERVICE, "getBranchDetail", () ->
				restaurantServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getBranchDetail(request));
	}

	public TableHierarchyResponse getTableHierarchyDetail(TableHierarchyRequest request) {
		return GrpcExecutor.execute(RESTAURANT_SERVICE, "getTableHierarchyDetail", () ->
				restaurantServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getTableHierarchyDetail(request));
	}
}