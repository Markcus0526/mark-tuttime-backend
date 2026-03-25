package com.tuttime.order.service;

import com.tuttime.order.dto.OrderItemDTO;

import java.util.List;
import java.util.UUID;

public interface OrderItemService {

	OrderItemDTO createOrderItem(OrderItemDTO orderItemDTO);

	OrderItemDTO updateOrderItem(UUID orderItemId, OrderItemDTO orderItemDTO);

	OrderItemDTO getOrderItemById(UUID orderItemId);

	List<OrderItemDTO> getItemsByOrderId(UUID orderId);

	void deleteOrderItem(UUID orderItemId);
}
