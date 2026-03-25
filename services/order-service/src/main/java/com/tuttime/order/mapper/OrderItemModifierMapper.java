package com.tuttime.order.mapper;

import com.tuttime.order.dto.OrderItemModifierDTO;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface OrderItemModifierMapper {
	OrderItemModifierDTO toDto(OrderItemModifier modifier);

	@Mapping(target = "orderItem", ignore = true)
	OrderItemModifier toEntity(OrderItemModifierDTO dto);

	@AfterMapping
	default void setBackReference(@MappingTarget OrderItemModifier modifier, @Context OrderItem orderItem) {
		if (orderItem != null) {
			modifier.setOrderItem(orderItem);
		}
	}
}
