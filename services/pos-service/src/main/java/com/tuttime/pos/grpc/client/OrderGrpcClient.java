package com.tuttime.pos.grpc.client;

import com.tuttime.common.proto.OrderDetailsResponse;
import com.tuttime.common.proto.OrderRequest;
import com.tuttime.common.proto.OrderServiceGrpc;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderGrpcClient {

	@GrpcClient("order-service") // Configuration name in application.yml
	private OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

	public OrderDetailsResponse fetchOrderDetails(UUID orderId) {
		// 1. Fetch full details via gRPC
		OrderRequest request = OrderRequest.newBuilder()
				.setOrderId(orderId.toString())
				.build();

		return orderServiceBlockingStub.getOrderDetails(request);
	}
}

