package com.tuttime.booking.service;

import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.common.event.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingGrpcDelegate {

	private final BookingRepository bookingRepository;

	@Transactional(readOnly = true)
	public GetCustomerRetentionResponse getCustomerRetention(GetCustomerRetentionRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		String period = request.getPeriod();
		LocalDate startDate = getStartDateForPeriod(period);

		OffsetDateTime startDateTime = startDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

		int newCustomers = bookingRepository.countFirstTimeCustomers(branchId, startDateTime);
		int repeatCustomers = bookingRepository.countRepeatCustomers(branchId, startDateTime);
		double repeatRate = (newCustomers + repeatCustomers) == 0 ? 0 :
				(repeatCustomers * 100.0 / (newCustomers + repeatCustomers));
		double avgOrders = bookingRepository.averageOrdersPerCustomer(branchId, startDateTime);

		return GetCustomerRetentionResponse.newBuilder()
				.setNewCustomers(newCustomers)
				.setRepeatCustomers(repeatCustomers)
				.setRepeatRate(repeatRate)
				.setAvgOrdersPerCustomer(avgOrders)
				.build();
	}

	@Transactional(readOnly = true)
	public GetFunnelCountsResponse getFunnelCounts(GetFunnelCountsRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		LocalDate start = LocalDate.parse(request.getStartDate());
		LocalDate end = LocalDate.parse(request.getEndDate());

		OffsetDateTime startDate = start.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
		OffsetDateTime endDate = end.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

		long bookings = bookingRepository.countBookings(branchId, startDate, endDate);
		long arrivals = bookingRepository.countArrivals(branchId, startDate, endDate);
		// For views, you might track via another source (e.g., analytics events)
		long views = 0;   // placeholder; implement if you have view tracking

		return GetFunnelCountsResponse.newBuilder()
				.setViews(views)
				.setBookings(bookings)
				.setArrivals(arrivals)
				.build();
	}

	@Transactional(readOnly = true)
	public GetTopCustomersResponse getTopCustomers(GetTopCustomersRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		int limit = request.getLimit();
		Pageable pageable = PageRequest.of(0, limit);
		Page<Object[]> results = bookingRepository.findTopCustomersBySpending(branchId, pageable);

		GetTopCustomersResponse.Builder responseBuilder = GetTopCustomersResponse.newBuilder();
		for (Object[] row : results.getContent()) {
			TopCustomer customer = TopCustomer.newBuilder()
					.setUserId(row[0].toString())
					.setTotalSpent(((BigDecimal) row[1]).doubleValue())
					.setBookingCount(((Number) row[2]).intValue())
					.build();
			responseBuilder.addCustomers(customer);
		}
		return responseBuilder.build();
	}

	private LocalDate getStartDateForPeriod(String period) {
		return switch (period.toUpperCase()) {
			case "WEEK" -> LocalDate.now().minusWeeks(1);
			case "MONTH" -> LocalDate.now().minusMonths(1);
			case "QUARTER" -> LocalDate.now().minusMonths(3);
			default -> LocalDate.now().minusMonths(1);
		};
	}
}
