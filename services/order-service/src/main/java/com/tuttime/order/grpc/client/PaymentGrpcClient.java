package com.tuttime.order.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.MENU_SERVICE;
import static com.tuttime.common.constants.ServiceNames.PAYMENT_SERVICE;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	// This name must match the name in application.yml
	@GrpcClient("payment-service")
	private PaymentServiceGrpc.PaymentServiceBlockingStub paymentServiceBlockingStub;

	public ProcessRefundResponse processRefund(ProcessRefundRequest request) {
		return GrpcExecutor.execute(PAYMENT_SERVICE, "processRefund", () ->
				paymentServiceBlockingStub.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.processRefund(request));
	}
}