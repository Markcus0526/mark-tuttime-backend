package com.tuttime.order.service;

import com.tuttime.order.dto.OrderItemModifierDTO;

import java.util.List;
import java.util.UUID;

public interface OrderItemModifierService {

	OrderItemModifierDTO createModifier(OrderItemModifierDTO modifierDTO);

	OrderItemModifierDTO updateModifier(UUID modifierId, OrderItemModifierDTO modifierDTO);

	OrderItemModifierDTO getModifierById(UUID modifierId);

	List<OrderItemModifierDTO> getModifiersByOrderItemId(UUID orderItemId);

	void deleteModifier(UUID modifierId);
}
