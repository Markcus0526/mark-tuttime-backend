package com.tuttime.analytics.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.BOOKING_SERVICE;
import static com.tuttime.common.constants.ServiceNames.MENU_SERVICE;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("booking-service")
	private BookingServiceGrpc.BookingServiceBlockingStub bookingServiceBlockingStub;

	private final GrpcExecutor grpcExecutor;

	public GetCustomerRetentionResponse getCustomerRetention(UUID branchId, String period) {
		GetCustomerRetentionRequest request = GetCustomerRetentionRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setPeriod(period)
				.build();

		return grpcExecutor.execute(BOOKING_SERVICE, "getCustomerRetention", () ->
				bookingServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getCustomerRetention(request));
	}

	public GetFunnelCountsResponse getFunnelCounts(UUID branchId, LocalDate start, LocalDate end) {
		GetFunnelCountsRequest request = GetFunnelCountsRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setStartDate(start.toString())
				.setEndDate(end.toString())
				.build();

		return grpcExecutor.execute(BOOKING_SERVICE, "getFunnelCounts", () ->
				bookingServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getFunnelCounts(request));
	}

	public GetTopCustomersResponse getTopCustomers(UUID branchId, int limit) {
		GetTopCustomersRequest request = GetTopCustomersRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setLimit(limit)
				.build();

		return grpcExecutor.execute(BOOKING_SERVICE, "getTopCustomers", () ->
				bookingServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getTopCustomers(request));
	}
}