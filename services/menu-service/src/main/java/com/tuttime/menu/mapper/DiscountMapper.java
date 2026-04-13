package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.entity.Discount;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DiscountMapper {

	Discount toEntity(DiscountDto.DiscountRequest request);

	DiscountDto.DiscountResponse toResponse(Discount entity);
}