package com.tuttime.booking.grpc.client;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.grpc.GrpcExecutor;
import com.tuttime.common.proto.CreateOrderRequest;
import com.tuttime.common.proto.OrderServiceGrpc;
import com.tuttime.common.util.UuidUtil;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.constants.ServiceNames.MENU_SERVICE;
import static com.tuttime.common.constants.ServiceNames.ORDER_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderGrpcClient {

	@GrpcClient("order-service")
	private OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub;

	public UUID createOrder(CreateOrderRequest request) {
		String orderId = GrpcExecutor.execute(ORDER_SERVICE, "createOrder", () ->
				orderServiceBlockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
						.createOrder(request).getOrderId());

		return UuidUtil.toUuid(orderId);
//		try {
//			orderServiceBlockingStub.createOrder(request);
//		} catch (StatusRuntimeException e) {
//			log.error("gRPC call to Order Service failed: {}", e.getStatus());
//			throw new InvalidRequestException("Could not initialize order: " + e.getStatus().getDescription());
//		}
	}
}