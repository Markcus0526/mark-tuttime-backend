package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.entity.Discount;
import com.tuttime.menu.mapper.DiscountMapper;
import com.tuttime.menu.repository.DiscountRepository;
import com.tuttime.menu.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

	private final DiscountRepository discountRepository;
	private final DiscountMapper discountMapper;

	@Override
	@Transactional
	public DiscountDto.DiscountResponse createDiscount(DiscountDto.DiscountRequest request, UUID currentUserId, String userRoles) {
		Discount discount = discountMapper.toEntity(request);
		Discount saved = discountRepository.save(discount);
		return discountMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public DiscountDto.DiscountResponse updateDiscount(UUID id, DiscountDto.DiscountRequest request, UUID currentUserId, String userRoles) {
		Discount existing = discountRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Discount not found with id: " + id));

		existing.setName(request.name());
		existing.setDiscountType(request.discountType());
		existing.setAmount(request.amount());
		existing.setAppliesTo(request.appliesTo());
		existing.setReferenceId(request.referenceId());
		existing.setStartDate(request.startDate());
		existing.setEndDate(request.endDate());
		existing.setActive(request.active() != null ? request.active() : existing.getActive());

		return discountMapper.toResponse(discountRepository.save(existing));
	}

	@Override
	@Transactional(readOnly = true)
	public DiscountDto.DiscountResponse getDiscountById(UUID id) {
		return discountRepository.findById(id)
				.map(discountMapper::toResponse)
				.orElseThrow(() -> new InvalidRequestException("Discount not found with id: " + id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DiscountDto.DiscountResponse> getActiveDiscountsForReference(UUID referenceId) {
		OffsetDateTime now = OffsetDateTime.now();

		// Logic: Find all discounts for this ID that are marked active
		// and where 'now' is between start and end dates (handling null dates as "always")
		return discountRepository.findByReferenceIdAndActiveTrue(referenceId)
				.stream()
				.filter(d -> (d.getStartDate() == null || d.getStartDate().isBefore(now)))
				.filter(d -> (d.getEndDate() == null || d.getEndDate().isAfter(now)))
				.map(discountMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void deleteDiscount(UUID id, UUID currentUserId, String userRoles) {
		if (!discountRepository.existsById(id)) {
			throw new InvalidRequestException("Discount not found with id: " + id);
		}
		discountRepository.deleteById(id);
	}
}