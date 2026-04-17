package com.tuttime.booking.grpc.client;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.proto.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.tuttime.common.util.DatetimeUtil.convertOffsetDatetimeToProtobuf;

@Service
@Slf4j
public class RestaurantGrpcClient {

	@GrpcClient("restaurant-service")
	private RestaurantServiceGrpc.RestaurantServiceBlockingStub restaurantServiceBlockingStub;

	public TableDetailResponse getTableDetails(UUID tableId) {
		TableDetailRequest request = TableDetailRequest.newBuilder()
				.setTableId(tableId.toString())
				.build();

		return restaurantServiceBlockingStub.getTableDetails(request);
	}

	public List<TableInfo> getTablesForBooking(UUID branchId, int partySize) {
		SlotAvailabilityRequest req = SlotAvailabilityRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setPartySize(partySize)
				.build();
		return restaurantServiceBlockingStub.getTablesForBooking(req).getTablesList();
	}

	public ScheduleResponse getBranchSchedule(UUID branchId, int dayOfWeek) {
		ScheduleRequest req = ScheduleRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setDayOfWeek(dayOfWeek)
				.build();
		return restaurantServiceBlockingStub.getBranchSchedule(req);
	}
}