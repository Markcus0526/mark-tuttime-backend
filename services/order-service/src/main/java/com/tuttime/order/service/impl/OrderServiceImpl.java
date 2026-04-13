package com.tuttime.order.service.impl;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.OrderEvent;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.order.dto.OrderRequest;
import com.tuttime.order.dto.OrderResponse;
import com.tuttime.order.dto
		.OrderUpdateRequest;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import com.tuttime.order.grpc.client.MenuGrpcClient;
import com.tuttime.order.mapper.OrderMapper;
import com.tuttime.order.repository.OrderRepository;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;
	private final EventProducer eventProducer;
	private final MenuGrpcClient menuGrpcClient;

	@Override
	public OrderResponse createOrder(OrderRequest request) {
		// 1. Initialize Order
		Order order = Order.builder()
				.bookingId(request.bookingId())
				.userId(request.userId())
				.status(OrderStatus.CREATED)
				.build();

		// 2. Map Request Records to Entities manually to ensure Business Rules
		request.items().forEach(itemReq -> {
			// gRPC Call: If this fails, the whole transaction rolls back
			BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(
					itemReq.menuItemId(),
					itemReq.modifiers().stream().map(m -> m.modifierId()).toList()
			);

			OrderItem item = OrderItem.builder()
					.menuItemId(itemReq.menuItemId())
					.quantity(itemReq.quantity())
					.price(verifiedPrice)
					.build();

			itemReq.modifiers().forEach(mod -> {
				item.getModifiers().add(OrderItemModifier.builder()
						.modifierId(mod.modifierId())
						.priceAdjustment(mod.priceAdjustment())
						.orderItem(item)
						.build());
			});
			order.addItem(item);
		});

		Order savedOrder = orderRepository.save(order);
		return orderMapper.toResponse(savedOrder);
	}

	@Override
	@Transactional(readOnly = true)
	public OrderResponse getOrderById(UUID id) {
		return orderRepository.findById(id)
				.map(orderMapper::toResponse)
				.orElseThrow(() -> new RuntimeException("Order not found: " + id));
	}

	@Override
	@Transactional(readOnly = true)
	public OrderResponse getOrderByBookingId(UUID bookingId) {
		return orderRepository.findByBookingId(bookingId)
				.map(orderMapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("No order found for booking: " + bookingId));
	}

	@Override
	@Transactional
	public OrderResponse updateOrderContent(UUID id, OrderUpdateRequest request) {
		// 1. Fetch with Pessimistic Lock to prevent others from modifying while we recalculate
		Order order = orderRepository.findByIdWithLock(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));

		// 2. Business Rule: Cannot modify order if already being prepared
		if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.ACCEPTED) {
			throw new ConflictException("Order content cannot be modified once preparation has started.");
		}

		// 3. Clear existing items (JPA orphanRemoval will delete them from DB)
		order.getItems().clear();

		// 4. Add new/updated items
		for (var itemReq : request.items()) {
			List<UUID> modIds = itemReq.modifiers().stream().map(m -> m.modifierId()).toList();

			// Verified Price via gRPC
			BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(itemReq.menuItemId(), modIds);

			OrderItem newItem = OrderItem.builder()
					.menuItemId(itemReq.menuItemId())
					.quantity(itemReq.quantity())
					.price(verifiedPrice)
					.order(order)
					.build();

			itemReq.modifiers().forEach(m -> {
				newItem.getModifiers().add(OrderItemModifier.builder()
						.modifierId(m.modifierId())
						.priceAdjustment(m.priceAdjustment())
						.orderItem(newItem)
						.build());
			});

			order.addItem(newItem);
		}

		// 5. Total is automatically recalculated inside addItem()
		Order updated = orderRepository.save(order);
		return orderMapper.toResponse(updated);
	}

	@Override
	@Transactional // CRITICAL: Pessimistic lock only works inside a transaction
	public OrderResponse updateStatus(UUID id, OrderStatus newStatus) {
		// 1. CALL THE LOCKING METHOD
		// This executes "SELECT ... FOR UPDATE" in Postgres
		Order order = orderRepository.findByIdWithLock(id)
				.orElseThrow(() -> new RuntimeException("Order not found"));

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
	public void deleteOrder(UUID id) {
		orderRepository.deleteById(id); // Triggers soft delete via @SQLDelete
	}

	@Override
	@Transactional
	public OrderResponse cancelOrder(UUID id) {
		Order order = orderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

		// Business Rule: Cannot cancel if food is already being served
		if (order.getStatus() == OrderStatus.SERVED || order.getStatus() == OrderStatus.COMPLETED) {
			throw new ConflictException("Order cannot be canceled in its current state.");
		}

		OrderStatus oldStatus = order.getStatus();
		order.setStatus(OrderStatus.CANCELED);
		Order updated = orderRepository.save(order);

		// Kafka Event
		eventProducer.sendOrderStatusUpdatedEvent(new OrderEvent.OrderStatusEvent(
				updated.getId(), updated.getBookingId(), oldStatus, OrderStatus.CANCELED, OffsetDateTime.now()
		));

		return orderMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public OrderResponse removeItem(UUID orderId, UUID itemId) {
		Order order = orderRepository.findByIdWithLock(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));

		// Business Rule check
		if (order.getStatus() != OrderStatus.CREATED) {
			throw new ConflictException("Cannot remove items after order is accepted");
		}

		// Find and remove the specific item
		boolean removed = order.getItems().removeIf(item -> item.getId().equals(itemId));

		if (!removed) {
			throw new ResourceNotFoundException("Item not found in this order");
		}

		// Recalculate total and save
		order.recalculateTotal();
		return orderMapper.toResponse(orderRepository.save(order));
	}

	@Override
	@Transactional
	public OrderResponse checkout(UUID id) {
		// 1. Lock the order to prevent concurrent modifications
		Order order = orderRepository.findByIdWithLock(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

		// 2. Validate State
		if (order.getStatus() != OrderStatus.CREATED) {
			throw new ConflictException("Order is in " + order.getStatus() + " state and cannot be paid.");
		}

		if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ConflictException("Cannot checkout an empty or zero-amount order.");
		}

		// 3. Logic: Integrate with Payment Service here (e.g., via Stripe/Braintree)
		// For now, we assume payment is successful.
		OrderStatus oldStatus = order.getStatus();
		order.setStatus(OrderStatus.PAID);

		Order savedOrder = orderRepository.save(order);

		// 4. Emit Kafka Event for POS and Analytics
		eventProducer.sendOrderStatusUpdatedEvent(new OrderEvent.OrderStatusEvent(
				savedOrder.getId(),
				savedOrder.getBookingId(),
				oldStatus,
				OrderStatus.PAID,
				OffsetDateTime.now()
		));

		return orderMapper.toResponse(savedOrder);
	}

	@Transactional
	public Order confirmOrderPayment(UUID bookingId) {
		// 1. Locate the specific order linked to this booking
		Order order = orderRepository.findByBookingId(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("No order found for booking: " + bookingId));

		// 2. Validate current state (prevent redundant updates or illegal transitions)
		if (order.getStatus() == OrderStatus.PAID) {
			log.info("Order for booking {} is already marked as PAID.", bookingId);
			return order;
		}

		// 3. Update status to PAID (or PLACED)
		order.setStatus(OrderStatus.PAID);

		// 4. Update audit fields (optional but recommended)
		order.setUpdatedAt(OffsetDateTime.now());

		// 5. Save and return for further processing (like pushing to POS)
		return orderRepository.save(order);
	}
}