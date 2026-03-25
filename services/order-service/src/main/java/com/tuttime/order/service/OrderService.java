package com.tuttime.order.service;

import com.tuttime.order.dto.OrderDto;

import java.util.List;
import java.util.UUID;


public interface OrderService {

	OrderDto createOrder(OrderDto orderDto);

	OrderDto updateOrder(UUID orderId, OrderDto orderDto);

	OrderDto getOrderById(UUID orderId);

	List<OrderDto> getOrdersByBookingId(UUID bookingId);

	List<OrderDto> getOrdersByUserId(UUID userId);

	void deleteOrder(UUID orderId);

	OrderDto updateOrderStatus(UUID orderId, String status);
}
