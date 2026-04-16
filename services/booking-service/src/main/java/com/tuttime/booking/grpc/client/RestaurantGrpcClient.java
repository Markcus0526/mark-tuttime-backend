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

	public AvailableTableResponse checkAvailableTableForBooking(BookingDto.BookingRequest bookingRequest) {
		AvailableTableRequest request = AvailableTableRequest.newBuilder()
				.setTableId(bookingRequest.tableId().toString())
				.setPartySize(bookingRequest.partySize())
				.setStartTime(convertOffsetDatetimeToProtobuf(bookingRequest.bookingDatetime()))
				.setDurationMinutes(bookingRequest.duration())
				.build();

		return restaurantServiceBlockingStub.checkAvailableTableForBooking(request);
	}

	public List<TableInfo> getTablesForBooking(UUID branchId, int partySize) {
		SlotAvailabilityRequest req = SlotAvailabilityRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setPartySize(partySize)
				.build();
		return restaurantServiceBlockingStub.getTablesForBooking(req).getTablesList();
	}
}