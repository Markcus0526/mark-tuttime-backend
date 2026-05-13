package com.tuttime.analytics.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.GetPaymentCountsRequest;
import com.tuttime.common.event.proto.GetPaymentCountsResponse;
import com.tuttime.common.event.proto.PaymentServiceGrpc;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.PAYMENT_SERVICE;

@Component
@RequiredArgsConstructor
public class PaymentGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("payment-service")
	private PaymentServiceGrpc.PaymentServiceBlockingStub paymentServiceBlockingStub;

	private final GrpcExecutor grpcExecutor;

	public GetPaymentCountsResponse getPaymentCounts(UUID branchId, LocalDate start, LocalDate end) {
		GetPaymentCountsRequest request = GetPaymentCountsRequest.newBuilder()
				.setBranchId(branchId.toString())
				.setStartDate(start.toString())
				.setEndDate(end.toString())
				.build();

		return grpcExecutor.execute(PAYMENT_SERVICE, "getPaymentCounts", () ->
				paymentServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getPaymentCounts(request));
	}
}
