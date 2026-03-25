package com.tuttime.order.mapper;

import com.tuttime.order.dto.OrderItemDto;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN, uses = OrderItemModifierMapper.class)
public interface OrderItemMapper {

	@Mapping(source = "modifiers", target = "modifiers")
	OrderItemDto toDto(OrderItem orderItem);

	@Mapping(target = "order", ignore = true)
	OrderItem toEntity(OrderItemDto dto);

	@AfterMapping
	default void setBackReference(@MappingTarget OrderItem item, @Context Order order) {
		if (order != null) {
			item.setOrder(order);
		}
	}
}
