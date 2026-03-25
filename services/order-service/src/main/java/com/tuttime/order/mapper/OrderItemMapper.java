package com.tuttime.order.mapper;

import com.tuttime.order.dto.OrderItemDTO;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN, uses = OrderItemModifierMapper.class)
public interface OrderItemMapper {

	@Mapping(source = "modifiers", target = "modifiers")
	OrderItemDTO toDto(OrderItem orderItem);

	@Mapping(target = "order", ignore = true)
	OrderItem toEntity(OrderItemDTO dto);

	@AfterMapping
	default void setBackReference(@MappingTarget OrderItem item, @Context Order order) {
		if (order != null) {
			item.setOrder(order);
		}
	}
}
