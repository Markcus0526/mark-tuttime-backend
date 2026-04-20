package com.tuttime.order.service;

import com.tuttime.order.dto.OrderDto;
import com.tuttime.common.enums.OrderStatus;
import com.tuttime.order.entity.Order;
import org.springframework.data.domain.Page;
import java.util.UUID;

public interface OrderService {

	// User Operations
	OrderDto.OrderResponse getOrderById(UUID orderId, UUID requesterId);
	Page<OrderDto.OrderResponse> getOrdersByUserId(UUID userId, int page, int size);
	OrderDto.OrderResponse getOrderByBookingId(UUID bookingId);
	OrderDto.OrderResponse updateOrderContent(UUID orderId, OrderDto.OrderUpdateRequest request, UUID userId);
	OrderDto.OrderResponse cancelOrder(UUID orderId, UUID userId);

	// Management & POS Operations
	OrderDto.OrderResponse updateStatus(UUID orderId, OrderStatus newStatus);
	OrderDto.OrderResponse syncStatusFromPos(UUID orderId, OrderDto.PosSyncRequest syncRequest);
	Page<OrderDto.OrderResponse> searchOrders(UUID branchId, OrderStatus status, int page, int size);

	void deleteOrder(UUID orderId); // Soft delete

	Order confirmOrderPayment(UUID bookingId, UUID userId, UUID branchId);

	void cancelOrderByBookingId(UUID bookingId);
}
