package com.tuttime.booking.grpc.client;

import com.tuttime.booking.dto.BookingRequest;
import com.tuttime.common.exception.BusinessException;
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

	public void checkAvailableTableForBooking(BookingRequest bookingRequest) {
		AvailableTableRequest request = AvailableTableRequest.newBuilder()
				.setRestaurantId(bookingRequest.restaurantId().toString())
				.setBranchId(bookingRequest.branchId().toString())
				.setZoneId(bookingRequest.zoneId().toString())
				.setTableId(bookingRequest.tableId().toString())
				.setPartySize(bookingRequest.partySize())
				.setStartTime(convertOffsetDatetimeToProtobuf(bookingRequest.bookingDatetime()))
				.setDurationMinutes(bookingRequest.duration())
				.build();

		AvailableTableResponse response = restaurantServiceBlockingStub.checkAvailableTableForBooking(request);

		if (!response.getIsAvailable()) {
			throw new ConflictException("You do not reserve this specific booking");
		}
	}

	public List<TableInfo> getTablesForBooking(UUID branchId, int partySize) {
		SlotAvailabilityRequest req = SlotAvailabilityRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setPartySize(partySize)
				.build();
		return restaurantServiceBlockingStub.getTablesForBooking(req).getTablesList();
	}
}