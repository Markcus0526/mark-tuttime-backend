package com.tuttime.booking.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.ORDER_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("order-service")
	private OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

	public CreateOrderResponse createOrder(CreateOrderRequest request) {
		return GrpcExecutor.execute(ORDER_SERVICE, "createOrder", () ->
				orderServiceBlockingStub.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.createOrder(request));
	}

	public UpdateOrderResponse updateOrder(UpdateOrderRequest request) {
		return GrpcExecutor.execute(ORDER_SERVICE, "updateOrder", () ->
				orderServiceBlockingStub.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.updateOrder(request));
	}

	public CancelOrderResponse cancelAndRefund(CancelOrderRequest request) {
		return GrpcExecutor.execute(ORDER_SERVICE, "cancelAndRefund", () ->
				orderServiceBlockingStub.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.cancelAndRefund(request));
	}
}