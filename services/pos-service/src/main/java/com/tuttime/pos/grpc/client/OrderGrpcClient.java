package com.tuttime.pos.grpc.client;

import com.tuttime.common.proto.OrderDetailRequest;
import com.tuttime.common.proto.OrderDetailResponse;
import com.tuttime.common.proto.OrderServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderGrpcClient {

	@GrpcClient("order-service") // Configuration name in application.yml
	private OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

	public OrderDetailResponse getOrderDetail(UUID orderId) {
		// 1. Fetch full details via gRPC
		OrderDetailRequest request = OrderDetailRequest.newBuilder()
				.setOrderId(orderId.toString())
				.build();

		return orderServiceBlockingStub.getOrderDetail(request);
	}
}

