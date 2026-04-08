package com.tuttime.booking.grpc.client;

import com.tuttime.common.proto.PaymentServiceGrpc;
import com.tuttime.common.proto.PaymentSessionRequest;
import com.tuttime.common.proto.PaymentSessionResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentGrpcClient {

	// This name must match the name in application.yml
	@GrpcClient("payment-service")
	private PaymentServiceGrpc.PaymentServiceBlockingStub paymentStub;

	public String getStripeUrl(UUID bookingId, UUID userId, BigDecimal amount) {
		// 1. Build the Request
		PaymentSessionRequest request = PaymentSessionRequest.newBuilder()
				.setBookingId(bookingId.toString())
				.setUserId(userId.toString())
				.setAmount(amount.doubleValue())
				.setCurrency("USD")
				.build();

		// 2. Execute synchronous gRPC call
		PaymentSessionResponse response = paymentStub.createCheckoutSession(request);

		// 3. Return the URL for the Flutter/React Native app
		return response.getCheckoutUrl();
	}
}