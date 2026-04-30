package com.tuttime.order.service.impl;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.event.OrderEvent;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.order.dto.OrderDto;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import com.tuttime.order.grpc.client.MenuGrpcClient;
import com.tuttime.order.grpc.client.RestaurantGrpcClient;
import com.tuttime.order.grpc.client.UserGrpcClient;
import com.tuttime.order.mapper.OrderMapper;
import com.tuttime.order.repository.OrderRepository;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.tuttime.common.util.UuidUtil.NIL_UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;
	private final EventProducer eventProducer;
	private final UserGrpcClient userGrpcClient;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final MenuGrpcClient menuGrpcClient;

	@Override
	@Transactional(readOnly = true)
	public OrderDto.OrderDetailResponse getOrderById(UUID orderId, UUID requesterId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		// 2. Fetch all external data in parallel
		return getEnrichedOrders(List.of(order)).getFirst();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<OrderDto.OrderDetailResponse> getOrdersByUserId(UUID userId, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

		// 1. Fetch the paginated entities
		Page<Order> orderPage = orderRepository.findByUserIdAndDeletedAtIsNull(userId, pageRequest);

		// 2. Extract content and enrich with User/Branch info
		// Assuming getEnrichedOrders returns List<OrderDetailResponse>
		List<OrderDto.OrderDetailResponse> enrichedList = getEnrichedOrders(orderPage.getContent());

		// 3. Wrap back into a Page object to maintain pagination metadata (total elements, total pages)
		return new PageImpl<>(enrichedList, pageRequest, orderPage.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public OrderDto.OrderDetailResponse getOrderByBookingId(UUID bookingId) {
		Order order = orderRepository.findByBookingIdWithItems(bookingId) // Use the fetch method
				.orElseThrow(() -> new InvalidRequestException("No order found for booking: " + bookingId));

		// 2. Fetch all external data in parallel
		return getEnrichedOrders(List.of(order)).getFirst();
	}

	@Override
	@Transactional
	public OrderDto.OrderResponse updateOrderContent(UUID orderId, OrderDto.OrderUpdateRequest request, UUID userId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		// Business Rule: Cannot edit if restaurant has already started (ACCEPTED/PREPARING)
		if (order.getStatus().ordinal() >= OrderStatus.ACCEPTED.ordinal()) {
			throw new InvalidRequestException("Cannot modify order after it has been accepted by the restaurant");
		}

		// 3. Clear existing items (JPA orphanRemoval handles DB cleanup)
		order.getItems().clear();

		// 4. Manually map and VERIFY prices (Don't trust the DTO prices!)
		for (var itemReq : request.items()) {
			// SAFE CHECK: Handle null modifiers list
			List<UUID> modIds = (itemReq.modifiers() != null)
					? itemReq.modifiers().stream().map(m -> m.modifierId()).toList()
					: Collections.emptyList();

			BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(itemReq.menuItemId(), modIds);

			OrderItem newItem = OrderItem.builder()
					.menuItemId(itemReq.menuItemId())
					.quantity(itemReq.quantity())
					.price(verifiedPrice) // Use verified price, not DTO price
					.order(order)
					.build();

			// SAFE LOOP: Only process if modifiers exist
			if (itemReq.modifiers() != null) {
				itemReq.modifiers().forEach(mod -> {
					newItem.getModifiers().add(OrderItemModifier.builder()
							.modifierId(mod.modifierId())
							.priceAdjustment(mod.priceAdjustment())
							.orderItem(newItem)
							.build());
				});
			}

			order.addItem(newItem);
		}

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
		OrderEvent.OrderStatusEvent event = OrderEvent.OrderStatusEvent.builder()
				.orderId(updatedOrder.getId())
				.bookingId(updatedOrder.getBookingId())
				.branchId(updatedOrder.getBranchId())
				.oldStatus(oldStatus)
				.newStatus(newStatus)
				.build();
		eventProducer.sendOrderStatusUpdatedEvent(event);

		return orderMapper.toResponse(orderRepository.save(order));
	}

	@Override
	@Transactional
	public OrderDto.OrderResponse cancelOrder(UUID orderId, UUID userId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		if (order.getStatus() == OrderStatus.SENT_TO_RESTAURANT || order.getStatus().ordinal() > 2) {
			throw new InvalidRequestException("Order is already with the kitchen and cannot be canceled");
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
		// "Your order is READY at the kitchen!" TODO
//		notificationService.sendOrderStatusUpdate(updated.getUserId(), updated.getStatus());

		return orderMapper.toResponse(updated);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<OrderDto.OrderDetailResponse> searchOrders(UUID branchId, OrderStatus status, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

		// 1. Fetch the paginated entities
		Page<Order> orderPage = orderRepository.searchOrders(branchId, status, pageRequest);

		// 2. Extract content and enrich with User/Branch info
		// Assuming getEnrichedOrders returns List<OrderDetailResponse>
		List<OrderDto.OrderDetailResponse> enrichedList = getEnrichedOrders(orderPage.getContent());

		// 3. Wrap back into a Page object to maintain pagination metadata (total elements, total pages)
		return new PageImpl<>(enrichedList, pageRequest, orderPage.getTotalElements());
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

	private List<OrderDto.OrderDetailResponse> getEnrichedOrders(List<Order> orders) {
		// 1. Collect all unique IDs to avoid redundant gRPC calls
		Set<String> userIds = orders.stream().map(b -> b.getUserId().toString()).collect(Collectors.toSet());
		Set<String> tableIds = orders.stream().map(b -> b.getTableId().toString()).collect(Collectors.toSet());
		List<String> menuItemIds = orders.stream()
				.flatMap(b -> b.getItems().stream().map(i -> i.getMenuItemId().toString()))
				.distinct().toList();

		// 2. Fetch all external data in parallel
		var usersFuture = CompletableFuture.supplyAsync(() -> fetchUsersMap(userIds));
		var tablesFuture = CompletableFuture.supplyAsync(() -> fetchTablesMap(tableIds));
		var menusFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return menuGrpcClient.getMenuDetails(menuItemIds).stream()
						.collect(Collectors.toMap(MenuItemDetailResponse::getMenuItemId, m -> m, (a, b) -> a));
			} catch (Exception e) {
				log.warn("Menu service failed, proceeding with empty menu data");
				return Collections.<String, MenuItemDetailResponse>emptyMap();
			}
		});

		CompletableFuture.allOf(usersFuture, tablesFuture, menusFuture).join();

		var userMap = usersFuture.join();
		var tableMap = tablesFuture.join();
		var menuMap = menusFuture.join();

		// 3. Map and return
		return orders.stream().map(booking -> {
			var user = userMap.getOrDefault(booking.getUserId().toString(),
					UserDetailResponse.newBuilder().setUserId(NIL_UUID).setFullName("Deleted User").build());

			var table = tableMap.getOrDefault(booking.getTableId().toString(),
					TableHierarchyResponse.newBuilder().setTableId(NIL_UUID).setTableLabel("N/A").build());

			var orderMenus = booking.getItems().stream()
					.map(i -> menuMap.get(i.getMenuItemId().toString()))
					.filter(Objects::nonNull)
					.toList();

			return orderMapper.toDetailResponse(booking, user, table, orderMenus);
		}).toList();
	}

	private Map<String, UserDetailResponse> fetchUsersMap(Set<String> ids) {
		return ids.parallelStream().collect(Collectors.toMap(
				id -> id,
				id -> {
					try {
						return userGrpcClient.getUserDetail(UserDetailRequest.newBuilder().setUserId(id).build());
					} catch (Exception e) {
						return UserDetailResponse.newBuilder().setUserId(NIL_UUID).build();
					}
				}
		));
	}

	private Map<String, TableHierarchyResponse> fetchTablesMap(Set<String> ids) {
		return ids.parallelStream().collect(Collectors.toMap(
				id -> id,
				id -> {
					try {
						return restaurantGrpcClient.getTableHierarchyDetail(TableHierarchyRequest.newBuilder().setTableId(id).build());
					} catch (Exception e) {
						return TableHierarchyResponse.newBuilder()
								.setTableId(NIL_UUID)      // Result: "00000000..."
								.setZoneId(NIL_UUID)       // Result: "00000000..."
								.setBranchId(NIL_UUID)     // Result: "00000000..."
								.setRestaurantId(NIL_UUID) // Result: "00000000..."
								.setTableLabel("N/A")
								.setZoneName("Unknown")
								.setRestaurantName("Unknown")
								.setBranchName("Unknown")
								.setBranchAddress("N/A")
								.setBranchPhone("N/A")
								.build();
					}
				}
		));
	}

}