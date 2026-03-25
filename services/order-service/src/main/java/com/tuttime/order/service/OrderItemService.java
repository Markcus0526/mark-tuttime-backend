package com.tuttime.order.service;

import com.tuttime.order.dto.OrderItemDto;

import java.util.List;
import java.util.UUID;

public interface OrderItemService {

	OrderItemDto createOrderItem(OrderItemDto orderItemDto);

	OrderItemDto updateOrderItem(UUID orderItemId, OrderItemDto orderItemDto);

	OrderItemDto getOrderItemById(UUID orderItemId);

	List<OrderItemDto> getItemsByOrderId(UUID orderId);

	void deleteOrderItem(UUID orderItemId);
}
