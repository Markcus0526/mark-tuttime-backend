package com.tuttime.order.service;

import com.tuttime.order.dto.OrderDTO;

import java.util.List;
import java.util.UUID;


public interface OrderService {

	OrderDTO createOrder(OrderDTO orderDTO);

	OrderDTO updateOrder(UUID orderId, OrderDTO orderDTO);

	OrderDTO getOrderById(UUID orderId);

	List<OrderDTO> getOrdersByBookingId(UUID bookingId);

	List<OrderDTO> getOrdersByUserId(UUID userId);

	void deleteOrder(UUID orderId);

	OrderDTO updateOrderStatus(UUID orderId, String status);
}
