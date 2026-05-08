package com.tuttime.payment.grpc;

import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.util.NumberUtil;
import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.Refund;
import com.tuttime.payment.service.PaymentService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;
import ru.loolzaaa.youkassa.pojo.Amount;
import ru.loolzaaa.youkassa.processors.RefundProcessor;

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
			String checkoutUrl = paymentService.initiatePaymentLifecycle(
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

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT
					.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND
					.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL
					.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void processRefund(ProcessRefundRequest request, StreamObserver<ProcessRefundResponse> responseObserver) {
		try {
			boolean status = paymentService.processAdministrativeRefund(
					UUID.fromString(request.getPaymentId()),
					NumberUtil.stringToBigDecimal(request.getAmount()),
					request.getReason());

			responseObserver.onNext(ProcessRefundResponse.newBuilder()
					.setStatus(status)
					.build());
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}
}