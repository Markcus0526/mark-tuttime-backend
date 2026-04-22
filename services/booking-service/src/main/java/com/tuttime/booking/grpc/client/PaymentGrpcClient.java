package com.tuttime.booking.grpc.client;

import com.tuttime.common.event.proto.PaymentServiceGrpc;
import com.tuttime.common.event.proto.PaymentSessionRequest;
import com.tuttime.common.event.proto.PaymentSessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentGrpcClient {

	// This name must match the name in application.yml
	@GrpcClient("payment-service")
	private PaymentServiceGrpc.PaymentServiceBlockingStub paymentServiceBlockingStub;

	public String getStripeUrl(UUID bookingId, UUID userId, UUID branchId, BigDecimal amount) {
		// 1. Build the Request
		PaymentSessionRequest request = PaymentSessionRequest.newBuilder()
				.setBookingId(bookingId.toString())
				.setUserId(userId.toString())
				.setBranchId(branchId.toString())
				.setAmount(amount.toString())
				.setCurrency("USD")
				.build();

		// 2. Execute synchronous gRPC call
		PaymentSessionResponse response = paymentServiceBlockingStub.createCheckoutSession(request);

		// 3. Return the URL for the Flutter/React Native app
		return response.getCheckoutUrl();
	}
}