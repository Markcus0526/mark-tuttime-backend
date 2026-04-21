package com.tuttime.payment.grpc;

import com.tuttime.common.proto.PaymentServiceGrpc;
import com.tuttime.common.proto.PaymentSessionRequest;
import com.tuttime.common.proto.PaymentSessionResponse;
import com.tuttime.common.util.NumberUtil;
import com.tuttime.payment.service.PaymentService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@Slf4j
@GrpcService // Tells Spring Boot to start the gRPC server on port 9090 (default)
@RequiredArgsConstructor
public class PaymentGrpcServer extends PaymentServiceGrpc.PaymentServiceImplBase {

	private final PaymentService paymentService;

	@Override
	public void createCheckoutSession(PaymentSessionRequest request,
	                                  StreamObserver<PaymentSessionResponse> responseObserver) {
		try {
			// 1. Business Logic call (uses Stripe)
			// We use the service we wrote earlier to handle the Stripe API call
			String checkoutUrl = paymentService.createCheckoutSession(
					UUID.fromString(request.getBookingId()),
					UUID.fromString(request.getUserId()),
					UUID.fromString(request.getBranchId()),
					NumberUtil.stringToBigDecimal(request.getAmount())
			);

			// 2. Build Response
			PaymentSessionResponse response = PaymentSessionResponse.newBuilder()
					.setCheckoutUrl(checkoutUrl)
					.setStatus("PENDING")
					.build();

			// 3. Complete gRPC Stream
			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (Exception e) {
			// Map Java exceptions to gRPC status codes
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to initiate Stripe session: " + e.getMessage())
					.asRuntimeException());
		}
	}
}