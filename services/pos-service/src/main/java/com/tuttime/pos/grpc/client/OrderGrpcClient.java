package com.tuttime.pos.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.OrderDetailRequest;
import com.tuttime.common.event.proto.OrderDetailResponse;
import com.tuttime.common.event.proto.OrderServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.ORDER_SERVICE;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderGrpcClient {

	@GrpcClient("order-service") // Configuration name in application.yml
	private OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

	public OrderDetailResponse getOrderDetail(UUID orderId) {
		OrderDetailRequest request = OrderDetailRequest.newBuilder()
				.setOrderId(orderId.toString())
				.build();

		return GrpcExecutor.execute(ORDER_SERVICE, "getOrderDetail", () ->
				orderServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.getOrderDetail(request));
	}
}

