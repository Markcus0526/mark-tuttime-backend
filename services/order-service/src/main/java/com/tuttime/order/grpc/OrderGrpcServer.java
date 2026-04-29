package com.tuttime.order.grpc;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import com.tuttime.order.grpc.client.MenuGrpcClient;
import com.tuttime.order.repository.OrderRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class OrderGrpcServer extends OrderServiceGrpc.OrderServiceImplBase {

	private final OrderRepository orderRepository;
	private final MenuGrpcClient menuGrpcClient;

	@Override
	@Transactional // Critical for database atomicity and gRPC rollback
	public void createOrder(CreateOrderRequest request, StreamObserver<CreateOrderResponse> responseObserver) {
		try {
			// 1. Initialize Order Aggregate Root
			Order order = Order.builder()
					.bookingId(UuidUtil.toUuid(request.getBookingId()))
					.userId(UuidUtil.toUuid(request.getUserId()))
					.branchId(UuidUtil.toUuid(request.getBranchId()))
					.tableId(UuidUtil.toUuid(request.getTableId()))
					.status(OrderStatus.CREATED)
					.items(new ArrayList<>()) // Ensure list is initialized
					.build();

			// 2. Map Request Items to Entities
			// Note: gRPC repeated fields use 'List' suffix in the getter
			for (OrderItemRequest itemReq : request.getItemsList()) {

				// Extract Modifier IDs for gRPC price verification
				List<UUID> modifierIds = itemReq.getModifiersList().stream()
						.map(m -> UuidUtil.toUuid(m.getModifierId()))
						.toList();

				// gRPC Call to Menu Service: Verified Price is the Source of Truth
				BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(
						UuidUtil.toUuid(itemReq.getMenuItemId()),
						modifierIds
				);

				// Create Order Item Entity
				OrderItem item = OrderItem.builder()
						.menuItemId(UuidUtil.toUuid(itemReq.getMenuItemId()))
						.quantity(itemReq.getQuantity())
						.price(verifiedPrice)
						.modifiers(new ArrayList<>())
						.order(order)
						.build();

				// Map Modifiers for this Item
				itemReq.getModifiersList().forEach(modReq -> {
					item.getModifiers().add(OrderItemModifier.builder()
							.modifierId(UuidUtil.toUuid(modReq.getModifierId()))
							.priceAdjustment(new BigDecimal(modReq.getPriceAdjustment()))
							.orderItem(item) // Maintain bidirectional link
							.build());
				});

				// Use the helper method to link item to order and handle bi-directionality
				order.addItem(item);
			}

			// 3. Recalculate Total on the server to prevent price spoofing
			order.calculateTotal();

			// 4. Persistence
			Order savedOrder = orderRepository.save(order);

			// 5. Build and Send Response
			CreateOrderResponse response = CreateOrderResponse.newBuilder()
					.setOrderId(savedOrder.getId().toString())
					.setStatus(savedOrder.getStatus().name())
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getOrderDetail(OrderDetailRequest request, StreamObserver<OrderDetailResponse> responseObserver) {
		try {
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

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}
}