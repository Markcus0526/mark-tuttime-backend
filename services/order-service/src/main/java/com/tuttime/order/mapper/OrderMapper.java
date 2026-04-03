package com.tuttime.order.mapper;

import com.tuttime.order.dto.OrderRequest;
import com.tuttime.order.dto.OrderResponse;
import com.tuttime.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

	OrderResponse toResponse(Order order);

	// Manual mapping for creation if needed,
	// though usually handled by business logic in Service
//	@Mapping(target = "id", ignore = true)
//	@Mapping(target = "status", constant = "CREATED")
	Order toEntity(OrderRequest request);
}
