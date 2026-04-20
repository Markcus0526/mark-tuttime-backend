package com.tuttime.booking.grpc.client;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.CreateOrderRequest;
import com.tuttime.common.proto.MenuServiceGrpc;
import com.tuttime.common.proto.OrderServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderGrpcClient {

	@GrpcClient("order-service")
	private OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

	public void createOrder(CreateOrderRequest request) {
		try {
			orderServiceBlockingStub.createOrder(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC call to Order Service failed: {}", e.getStatus());
			throw new InvalidRequestException("Could not initialize order: " + e.getStatus().getDescription());
		}
	}
}