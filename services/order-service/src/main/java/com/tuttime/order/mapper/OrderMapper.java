package com.tuttime.order.mapper;

import com.tuttime.order.dto.OrderDto;
import com.tuttime.order.entity.Order;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

	@Mapping(source = "orderItems", target = "orderItems")
    OrderDto toDto(Order order);

	@Mapping(target = "orderItems", ignore = true)
	Order toEntity(OrderDto dto);

	@AfterMapping
	@Mapping(target = "orderItems", ignore = true)
	default void calculateTotal(@MappingTarget OrderDto dto, Order order) {
		if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
			BigDecimal total = order.getOrderItems().stream()
					.map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			dto.setTotalAmount(total);
		}
	}
}
