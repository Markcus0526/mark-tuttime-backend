package com.tuttime.order.service;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import com.tuttime.order.grpc.client.MenuGrpcClient;
import com.tuttime.order.grpc.client.PaymentGrpcClient;
import com.tuttime.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderGrpcDelegate {

	private final OrderRepository orderRepository;
	private final MenuGrpcClient menuGrpcClient;
	private final PaymentGrpcClient paymentGrpcClient;

	@Transactional
	public CreateOrderResponse createOrder(CreateOrderRequest request) {
		Order order = Order.builder()
				.bookingId(UuidUtil.toUuid(request.getBookingId()))
				.userId(UuidUtil.toUuid(request.getUserId()))
				.branchId(UuidUtil.toUuid(request.getBranchId()))
				.tableId(UuidUtil.toUuid(request.getTableId()))
				.guestName(request.getGuestName())
				.guestPhone(request.getGuestPhone())
				.specialRequest(request.getSpecialRequest())
				.status(OrderStatus.CREATED)
				.items(new ArrayList<>())
				.build();

		for (OrderItemRequest itemReq : request.getItemsList()) {
			List<UUID> modifierIds = itemReq.getModifiersList().stream()
					.map(m -> UuidUtil.toUuid(m.getModifierId()))
					.toList();

			BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(
					UuidUtil.toUuid(itemReq.getMenuItemId()),
					modifierIds
			);

			OrderItem item = OrderItem.builder()
					.menuItemId(UuidUtil.toUuid(itemReq.getMenuItemId()))
					.quantity(itemReq.getQuantity())
					.price(verifiedPrice)
					.modifiers(new ArrayList<>())
					.order(order)
					.build();

			itemReq.getModifiersList().forEach(modReq -> {
				item.getModifiers().add(OrderItemModifier.builder()
						.modifierId(UuidUtil.toUuid(modReq.getModifierId()))
						.priceAdjustment(new BigDecimal(modReq.getPriceAdjustment()))
						.orderItem(item)
						.build());
			});

			order.addItem(item);
		}

		order.calculateTotal();
		Order savedOrder = orderRepository.save(order);

		return CreateOrderResponse.newBuilder()
				.setOrderId(savedOrder.getId().toString())
				.setStatus(savedOrder.getStatus().name())
				.build();
	}

	@Transactional
	public UpdateOrderResponse updateOrder(UpdateOrderRequest request) {
		Order order = orderRepository.findById(UuidUtil.toUuid(request.getOrderId()))
				.orElseThrow(() -> new InvalidRequestException("Order not found: " + request.getOrderId()));

		if (order.getStatus() == OrderStatus.SERVED || order.getStatus() == OrderStatus.COMPLETED) {
			throw new InvalidRequestException("Cannot edit an order that is already served or completed.");
		}

		order.setGuestName(request.getGuestName());
		order.setGuestPhone(request.getGuestPhone());
		order.setSpecialRequest(request.getSpecialRequest());

		// Clear existing items (orphan removal should cascade)
		order.getItems().clear();

		for (OrderItemRequest itemReq : request.getItemsList()) {
			List<UUID> modifierIds = itemReq.getModifiersList().stream()
					.map(m -> UuidUtil.toUuid(m.getModifierId()))
					.toList();

			BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(
					UuidUtil.toUuid(itemReq.getMenuItemId()),
					modifierIds
			);

			OrderItem item = OrderItem.builder()
					.menuItemId(UuidUtil.toUuid(itemReq.getMenuItemId()))
					.quantity(itemReq.getQuantity())
					.price(verifiedPrice)
					.modifiers(new ArrayList<>())
					.order(order)
					.build();

			itemReq.getModifiersList().forEach(modReq -> {
				item.getModifiers().add(OrderItemModifier.builder()
						.modifierId(UuidUtil.toUuid(modReq.getModifierId()))
						.priceAdjustment(new BigDecimal(modReq.getPriceAdjustment()))
						.orderItem(item)
						.build());
			});

			order.addItem(item);
		}

		order.calculateTotal();
		orderRepository.save(order);

		return UpdateOrderResponse.newBuilder()
				.setSuccess(true)
				.setMessage("Order updated successfully")
				.build();
	}

	@Transactional(readOnly = true)
	public OrderDetailResponse getOrderDetail(OrderDetailRequest request) {
		Order order = orderRepository.findById(UUID.fromString(request.getOrderId()))
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		OrderDetailResponse.Builder responseBuilder = OrderDetailResponse.newBuilder()
				.setOrderId(order.getId().toString())
				.setBookingId(order.getBookingId().toString())
				.setTotalAmount(order.getTotalAmount().doubleValue());

		// In real code, fetch menu item names via gRPC to MenuService
		order.getItems().forEach(item -> {
			responseBuilder.addItems(OrderItemDetailResponse.newBuilder()
					.setMenuItemName("Burger") // placeholder
					.setQuantity(item.getQuantity())
					.setPrice(item.getPrice().doubleValue())
					.build());
		});

		return responseBuilder.build();
	}

	@Transactional
	public CancelOrderResponse cancelAndRefund(CancelOrderRequest request) {
		Order order = orderRepository.findById(UUID.fromString(request.getOrderId()))
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		CancelOrderResponse.Builder responseBuilder = CancelOrderResponse.newBuilder();

		if (order.getStatus() == OrderStatus.CANCELED) {
			return responseBuilder.setSuccess(false).setReason("Order is canceled.").build();
		}

		if (order.getStatus() == OrderStatus.SERVED || order.getStatus() == OrderStatus.COMPLETED) {
			return responseBuilder.setSuccess(false).setReason("Order is served or completed now.").build();
		}

		ProcessRefundRequest refundRequest = ProcessRefundRequest.newBuilder()
				.setBookingId(request.getBookingId())
				.setPaymentId(request.getPaymentId())
				.setAmount(order.getTotalAmount().toString())
				.setReason(StringUtils.hasText(request.getReason()) ? request.getReason() : "Canceled by user")
				.build();
		ProcessRefundResponse refundResponse = paymentGrpcClient.processRefund(refundRequest);

		if (refundResponse.getStatus()) {
			order.setStatus(OrderStatus.CANCELED);
			orderRepository.save(order);
		}

		responseBuilder.setSuccess(refundResponse.getStatus());
		if (!refundResponse.getStatus()) {
			responseBuilder.setReason("Refund is failed by Yoomoney.");
		}
		return responseBuilder.build();
	}
}