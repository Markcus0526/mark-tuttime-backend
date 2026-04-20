package com.tuttime.order.service.impl;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.OrderEvent;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.order.dto.OrderDto;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import com.tuttime.order.grpc.client.MenuGrpcClient;
import com.tuttime.order.mapper.OrderMapper;
import com.tuttime.order.repository.OrderRepository;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;
	private final EventProducer eventProducer;
	private final MenuGrpcClient menuGrpcClient;

	@Override
	@Transactional(readOnly = true)
	public OrderDto.OrderResponse getOrderById(UUID orderId, UUID requesterId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		// Logical Security Check
		if (!order.getUserId().equals(requesterId)) {
			throw new RuntimeException("Access Denied");
		}
		return orderMapper.toResponse(order);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<OrderDto.OrderResponse> getOrdersByUserId(UUID userId, int page, int size) {
		return orderRepository.findByUserIdAndDeletedAtIsNull(userId, PageRequest.of(page, size))
				.map(orderMapper::toResponse);
	}

	@Override
	public OrderDto.OrderResponse getOrderByBookingId(UUID bookingId) {
		return orderRepository.findByBookingIdAndDeletedAtIsNull(bookingId)
				.map(orderMapper::toResponse)
				.orElseThrow(() -> new InvalidRequestException("No order found for booking: " + bookingId));
	}

	@Override
	@Transactional
	public OrderDto.OrderResponse updateOrderContent(UUID orderId, OrderDto.OrderUpdateRequest request, UUID userId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		// Business Rule: Cannot edit if restaurant has already started (ACCEPTED/PREPARING)
		if (order.getStatus().ordinal() >= OrderStatus.ACCEPTED.ordinal()) {
			throw new IllegalStateException("Cannot modify order after it has been accepted by the restaurant");
		}

		// Map DTO to Entity (Clears old Items and adds new ones via Cascade)
		orderMapper.updateEntityFromDto(request, order);

		// RECALCULATION: Crucial for your NUMERIC(12,2) total_amount column
		recalculateTotal(order);

		return orderMapper.toResponse(orderRepository.save(order));
	}

	@Override
	@Transactional // CRITICAL: Pessimistic lock only works inside a transaction
	public OrderDto.OrderResponse updateStatus(UUID id, OrderStatus newStatus) {
		// 1. CALL THE LOCKING METHOD
		// This executes "SELECT ... FOR UPDATE" in Postgres
		Order order = orderRepository.findByIdWithLock(id)
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		// 2. Perform logic while the DB row is LOCKED
		OrderStatus oldStatus = order.getStatus();
		order.setStatus(newStatus);

		// 3. Save and Commit
		// The lock is released automatically when this method finishes (Transaction ends)
		Order updatedOrder = orderRepository.save(order);

		// Publish Event to Kafka
		OrderEvent.OrderStatusEvent event = new OrderEvent.OrderStatusEvent(
				updatedOrder.getId(),
				updatedOrder.getBookingId(),
				oldStatus,
				newStatus,
				OffsetDateTime.now()
		);
		eventProducer.sendOrderStatusUpdatedEvent(event);

		return orderMapper.toResponse(orderRepository.save(order));
	}

	@Override
	@Transactional
	public OrderDto.OrderResponse cancelOrder(UUID orderId, UUID userId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		if (order.getStatus() == OrderStatus.SENT_TO_RESTAURANT || order.getStatus().ordinal() > 2) {
			throw new IllegalStateException("Order is already with the kitchen and cannot be canceled");
		}

		order.setStatus(OrderStatus.CANCELED);
		return orderMapper.toResponse(orderRepository.save(order));
	}

	@Override
	@Transactional
	public OrderDto.OrderResponse syncStatusFromPos(UUID orderId, OrderDto.PosSyncRequest request) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		// Update status based on POS feedback
		order.setStatus(request.posStatus());

		// Save the update
		Order updated = orderRepository.save(order);

		// CRITICAL: Trigger Real-time notification (WebSocket)
		// "Your order is READY at the kitchen!"
//		notificationService.sendOrderStatusUpdate(updated.getUserId(), updated.getStatus());

		return orderMapper.toResponse(updated);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<OrderDto.OrderResponse> searchOrders(UUID branchId, OrderStatus status, int page, int size) {
		// Using a custom repository method for dynamic filtering
		return orderRepository.searchOrders(branchId, status, PageRequest.of(page, size))
				.map(orderMapper::toResponse);
	}

	@Override
	@Transactional
	public void deleteOrder(UUID id) {
		orderRepository.deleteById(id); // Triggers soft delete via @SQLDelete
	}

	@Override
	@Transactional
	public Order confirmOrderPayment(UUID bookingId, UUID userId, UUID branchId) {
		// 1. Try to find the existing order
		return orderRepository.findByBookingIdAndDeletedAtIsNull(bookingId)
				.map(order -> {
					// 2. If it's already PAID or further along, don't repeat the logic
					if (order.getStatus().ordinal() >= OrderStatus.PAID.ordinal()) {
						log.warn("Order for booking {} is already in status: {}", bookingId, order.getStatus());
						return order;
					}

					// 3. Update status to PAID
					order.setStatus(OrderStatus.PAID);
					log.info("Transitioning order {} to PAID for booking {}", order.getId(), bookingId);
					return orderRepository.save(order);
				})
				.orElseGet(() -> {
					// 4. FALLBACK: If order doesn't exist yet (Race condition), create it.
					// NOTE: You need to ensure the Payment event contains userId and branchId
					log.info("Order not found for booking {}. Creating new PAID order.", bookingId);

					Order newOrder = Order.builder()
							.bookingId(bookingId)
							// These must come from your event/context
							.userId(userId)
							.branchId(branchId)
							.status(OrderStatus.PAID)
							.totalAmount(BigDecimal.ZERO)
							.build();

					return orderRepository.save(newOrder);
				});
	}

	@Override
	@Transactional
	public void cancelOrderByBookingId(UUID bookingId) {
		log.info("Attempting to cancel order for booking: {}", bookingId);

		// 1. Find the order by the logical booking ID
		Order order = orderRepository.findByBookingIdAndDeletedAtIsNull(bookingId)
				.orElseThrow(() -> new InvalidRequestException("No active order found for booking: " + bookingId));

		// 2. Business Rule: Safety Check
		// If the order is already PREPARING, READY, or SERVED, don't allow auto-cancellation
		if (order.getStatus().ordinal() >= OrderStatus.PREPARING.ordinal()) {
			log.warn("Cannot auto-cancel order {}. It is already in status: {}", order.getId(), order.getStatus());
			// You might want to trigger a manual alert for staff here
			return;
		}

		// 3. Perform Cancellation
		order.setStatus(OrderStatus.CANCELED);
		orderRepository.save(order);

		log.info("Order {} successfully canceled due to booking cancellation event.", order.getId());
	}

	/**
	 * Logic to sum Items and their Modifiers to update the Order total_amount
	 */
	private void recalculateTotal(Order order) {
		BigDecimal total = order.getItems().stream()
				.map(item -> {
					BigDecimal itemBase = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
					BigDecimal modifiersTotal = item.getModifiers().stream()
							.map(m -> m.getPriceAdjustment().multiply(BigDecimal.valueOf(item.getQuantity())))
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					return itemBase.add(modifiersTotal);
				})
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		order.setTotalAmount(total);
	}



}