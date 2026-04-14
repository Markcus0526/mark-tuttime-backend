package com.tuttime.menu.service;

import com.tuttime.menu.dto.DiscountDto;

import java.util.List;
import java.util.UUID;

public interface DiscountService {
	DiscountDto.DiscountResponse createDiscount(DiscountDto.DiscountRequest request, UUID currentUserId, String userRoles);

	DiscountDto.DiscountResponse updateDiscount(UUID id, DiscountDto.DiscountRequest request, UUID currentUserId, String userRoles);

	DiscountDto.DiscountResponse getDiscountById(UUID id);

	List<DiscountDto.DiscountResponse> getActiveDiscountsForReference(UUID referenceId);

	void deleteDiscount(UUID id, UUID currentUserId, String userRoles);
}