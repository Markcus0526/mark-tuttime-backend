package com.tuttime.analytics.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.BusinessException;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.ORDER_SERVICE;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("order-service")
	private OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

	private final GrpcExecutor grpcExecutor;

	public GetTopItemsResponse getTopItems(UUID branchId, LocalDate from, LocalDate to, int limit) {
		GetTopItemsRequest request = GetTopItemsRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setFromDate(from.toString())
				.setToDate(to.toString())
				.setLimit(limit)
				.build();

		return grpcExecutor.execute(ORDER_SERVICE, "getTopItems", () ->
				orderServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getTopItems(request));
	}

	public GetRevenueBreakdownResponse getRevenueBreakdown(UUID branchId, String granularity, LocalDate date) {
		GetRevenueBreakdownRequest request = GetRevenueBreakdownRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setGranularity(granularity)
				.setDate(date.toString())
				.build();

		return grpcExecutor.execute(ORDER_SERVICE, "getRevenueBreakdown", () ->
				orderServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getRevenueBreakdown(request));
	}

	public GetMenuPerformanceResponse getMenuPerformance(UUID branchId, LocalDate start, LocalDate end, String groupBy) {
		GetMenuPerformanceRequest request = GetMenuPerformanceRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setStartDate(start.toString())
				.setEndDate(end.toString())
				.setGroupBy(groupBy)
				.build();

		return grpcExecutor.execute(ORDER_SERVICE, "getMenuPerformance", () ->
				orderServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getMenuPerformance(request));
	}
}