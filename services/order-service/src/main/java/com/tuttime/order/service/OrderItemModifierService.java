package com.tuttime.order.service;

import com.tuttime.order.dto.OrderItemModifierDto;

import java.util.List;
import java.util.UUID;

public interface OrderItemModifierService {

	OrderItemModifierDto createModifier(OrderItemModifierDto modifierDTO);

	OrderItemModifierDto updateModifier(UUID modifierId, OrderItemModifierDto modifierDTO);

	OrderItemModifierDto getModifierById(UUID modifierId);

	List<OrderItemModifierDto> getModifiersByOrderItemId(UUID orderItemId);

	void deleteModifier(UUID modifierId);
}
