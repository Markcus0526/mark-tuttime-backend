package com.tuttime.pos.grpc.client;

import com.tuttime.common.proto.OrderDetailsResponse;
import com.tuttime.common.proto.OrderRequest;
import com.tuttime.common.proto.OrderServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderGrpcClient {

	@GrpcClient("order-service") // Configuration name in application.yml
	private OrderServiceGrpc.OrderServiceBlockingStub orderStub;

	public OrderDetailsResponse fetchOrderDetails(UUID orderId) {
		// 1. Fetch full details via gRPC
		OrderRequest request = OrderRequest.newBuilder()
				.setOrderId(orderId.toString())
				.build();

		return orderStub.getOrderDetails(request);
	}
}

