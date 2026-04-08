package com.tuttime.order.grpc;

import com.tuttime.common.proto.OrderDetailsResponse;
import com.tuttime.common.proto.OrderItemDetail;
import com.tuttime.common.proto.OrderRequest;
import com.tuttime.common.proto.OrderServiceGrpc;
import com.tuttime.order.entity.Order;
import com.tuttime.order.repository.OrderRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class OrderGrpcServer extends OrderServiceGrpc.OrderServiceImplBase {

	private final OrderRepository orderRepository;

	@Override
	public void getOrderDetails(OrderRequest request, StreamObserver<OrderDetailsResponse> responseObserver) {
		Order order = orderRepository.findById(UUID.fromString(request.getOrderId()))
				.orElseThrow(() -> new RuntimeException("Order not found"));

		OrderDetailsResponse.Builder responseBuilder = OrderDetailsResponse.newBuilder()
				.setOrderId(order.getId().toString())
				.setBookingId(order.getBookingId().toString())
				.setTotalAmount(order.getTotalAmount().doubleValue());

		// Map items to Proto format
		order.getItems().forEach(item -> {
			responseBuilder.addItems(OrderItemDetail.newBuilder()
					.setMenuItemName("Burger") // Ideally fetched from Menu Service
					.setQuantity(item.getQuantity())
					.setPrice(item.getPrice().doubleValue())
					.build());
		});

		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}
}