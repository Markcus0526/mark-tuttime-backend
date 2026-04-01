package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.entity.Discount;
import org.springframework.stereotype.Component;

@Component
public class DiscountMapper {

	public Discount toEntity(DiscountDto.DiscountRequest request) {
		return Discount.builder()
				.name(request.name())
				.discountType(request.discountType())
				.amount(request.amount())
				.appliesTo(request.appliesTo())
				.referenceId(request.referenceId())
				.startDate(request.startDate())
				.endDate(request.endDate())
				.isActive(request.isActive() != null ? request.isActive() : true)
				.build();
	}

	public DiscountDto.DiscountResponse toResponse(Discount o) {
		return new DiscountDto.DiscountResponse(
				o.getId(),
				o.getName(),
				o.getDiscountType(),
				o.getAmount(),
				o.getAppliesTo(),
				o.getReferenceId(),
				o.getStartDate(),
				o.getEndDate(),
				o.getIsActive()
		);
	}
}