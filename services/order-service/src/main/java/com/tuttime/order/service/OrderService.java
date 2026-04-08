package com.tuttime.order.service;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.order.dto.OrderRequest;
import com.tuttime.order.dto.OrderResponse;
import com.tuttime.order.dto.OrderUpdateRequest;
import com.tuttime.order.entity.Order;

import java.util.UUID;

public interface OrderService {

	OrderResponse createOrder(OrderRequest request);

	OrderResponse updateOrderContent(UUID id, OrderUpdateRequest request);

	OrderResponse getOrderById(UUID id);

	OrderResponse getOrderByBookingId(UUID bookingId);

	OrderResponse updateStatus(UUID id, OrderStatus status);

	void deleteOrder(UUID id);

	OrderResponse cancelOrder(UUID id);

	OrderResponse removeItem(UUID orderId, UUID itemId);

	OrderResponse checkout(UUID id);

	Order confirmOrderPayment(UUID bookingId);
}