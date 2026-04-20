package com.tuttime.order.grpc;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.*;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import com.tuttime.order.repository.OrderRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class OrderGrpcServer extends OrderServiceGrpc.OrderServiceImplBase {

	private final OrderRepository orderRepository;

	@Override
	public void createOrder(CreateOrderRequest request, StreamObserver<CreateOrderResponse> responseObserver) {
		// 1. Initialize Order
		Order order = Order.builder()
				.bookingId(UuidUtil.toUuid(request.getBookingId()))
				.userId(UuidUtil.toUuid(request.getUserId()))
				.branchId(UuidUtil.toUuid(request.getBranchId()))
				.totalAmount(BigDecimal.valueOf(request.getTotalAmount()))
				.status(OrderStatus.CREATED)
				.build();

		// 2. Map Request Records to Entities manually to ensure Business Rules TODO
//		request.items().forEach(itemReq -> {
//			// gRPC Call: If this fails, the whole transaction rolls back
//			BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(
//					itemReq.menuItemId(),
//					itemReq.modifiers().stream().map(m -> m.modifierId()).toList()
//			);
//
//			OrderItem item = OrderItem.builder()
//					.menuItemId(itemReq.menuItemId())
//					.quantity(itemReq.quantity())
//					.price(verifiedPrice)
//					.build();
//
//			itemReq.modifiers().forEach(mod -> {
//				item.getModifiers().add(OrderItemModifier.builder()
//						.modifierId(mod.modifierId())
//						.priceAdjustment(mod.priceAdjustment())
//						.orderItem(item)
//						.build());
//			});
//			order.addItem(item);
//		});
//
		Order savedOrder = orderRepository.save(order);

		CreateOrderResponse response = CreateOrderResponse.newBuilder()
				.setOrderId(savedOrder.getId().toString())
				.setStatus(savedOrder.getStatus().name())
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void getOrderDetail(OrderDetailRequest request, StreamObserver<OrderDetailResponse> responseObserver) {
		Order order = orderRepository.findById(UUID.fromString(request.getOrderId()))
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		OrderDetailResponse.Builder responseBuilder = OrderDetailResponse.newBuilder()
				.setOrderId(order.getId().toString())
				.setBookingId(order.getBookingId().toString())
				.setTotalAmount(order.getTotalAmount().doubleValue());

		// Map items to Proto format
		order.getItems().forEach(item -> {
			responseBuilder.addItems(OrderItemDetailResponse.newBuilder()
					.setMenuItemName("Burger") // Ideally fetched from Menu Service
					.setQuantity(item.getQuantity())
					.setPrice(item.getPrice().doubleValue())
					.build());
		});

		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}
}