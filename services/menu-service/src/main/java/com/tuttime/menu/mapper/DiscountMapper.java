package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.entity.Discount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DiscountMapper {

	Discount toEntity(DiscountDto.DiscountRequest request);

	@Mapping(source = "id", target = "discountId")
	DiscountDto.DiscountResponse toResponse(Discount entity);
}