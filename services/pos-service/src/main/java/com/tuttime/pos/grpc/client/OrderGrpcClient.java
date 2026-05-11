package com.tuttime.pos.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.OrderDetailRequest;
import com.tuttime.common.event.proto.OrderDetailResponse;
import com.tuttime.common.event.proto.OrderServiceGrpc;
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

	@GrpcClient("order-service") // Configuration name in application.yml
	private OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

	private final GrpcExecutor grpcExecutor;

	public OrderDetailResponse getOrderDetail(UUID orderId) {
		OrderDetailRequest request = OrderDetailRequest.newBuilder()
				.setOrderId(orderId.toString())
				.build();

		return grpcExecutor.execute(ORDER_SERVICE, "getOrderDetail", () ->
				orderServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.getOrderDetail(request));
	}
}

