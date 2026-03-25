package com.tuttime.order.mapper;

import java.math.BigDecimal;

import com.tuttime.order.dto.OrderDTO;
import com.tuttime.order.entity.Order;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN, uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(source = "orderItems", target = "orderItems")
    OrderDTO toDto(Order order);

    @Mapping(target = "orderItems", ignore = true)
    Order toEntity(OrderDTO dto);

    @AfterMapping
    @Mapping(target = "orderItems", ignore = true)
    default void calculateTotal(@MappingTarget OrderDTO dto, Order order) {
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            BigDecimal total = order.getOrderItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalAmount(total);
        }
    }
}
