package com.tuttime.order.mapper;

import com.tuttime.order.dto.OrderItemModifierDto;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface OrderItemModifierMapper {
	OrderItemModifierDto toDto(OrderItemModifier modifier);

	@Mapping(target = "orderItem", ignore = true)
	OrderItemModifier toEntity(OrderItemModifierDto dto);

	@AfterMapping
	default void setBackReference(@MappingTarget OrderItemModifier modifier, @Context OrderItem orderItem) {
		if (orderItem != null) {
			modifier.setOrderItem(orderItem);
		}
	}
}
