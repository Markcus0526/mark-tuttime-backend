package com.tuttime.booking.grpc.client;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

		BranchPermissionResponse response = restaurantServiceBlockingStub.validateBranchPermission(request);

		if (!response.getValid()) {
			throw new InvalidRequestException("You do not manage this specific menu");
		}
	}

	public TableDetailResponse getTableDetail(UUID tableId) {
		TableDetailRequest request = TableDetailRequest.newBuilder()
				.setTableId(tableId.toString())
				.build();

		return restaurantServiceBlockingStub.getTableDetail(request);
	}

	public List<TableSlotResponse> getBranchSlots(UUID branchId, int partySize) {
		BranchSlotsRequest req = BranchSlotsRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setPartySize(partySize)
				.build();
		return restaurantServiceBlockingStub.getBranchSlots(req).getTablesList();
	}

	public ScheduleResponse getBranchSchedule(UUID branchId, int dayOfWeek) {
		ScheduleRequest req = ScheduleRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setDayOfWeek(dayOfWeek)
				.build();
		return restaurantServiceBlockingStub.getBranchSchedule(req);
	}

	public BranchDetailResponse getBranchDetail(UUID branchId) {
		BranchDetailRequest req = BranchDetailRequest.newBuilder()
				.setBranchId(branchId.toString())
				.build();
		return restaurantServiceBlockingStub.getBranchDetail(req);
	}

	public TableHierarchyResponse getTableHierarchyDetail(TableHierarchyRequest request) {
		return restaurantServiceBlockingStub.getTableHierarchyDetail(request);
	}
}