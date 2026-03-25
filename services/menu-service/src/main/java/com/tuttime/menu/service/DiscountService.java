package com.tuttime.menu.service;

import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.entity.Discount;
import com.tuttime.menu.exception.ResourceNotFoundException;
import com.tuttime.menu.mapper.MenuMapper;
import com.tuttime.menu.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountService {
	private final DiscountRepository discountRepository;

	public List<DiscountDto> getByReferenceId(Long referenceId) {
		return discountRepository.findByReferenceId(referenceId)
				.stream()
				.map(MenuMapper::toDto)
				.collect(Collectors.toList());
	}

	public DiscountDto getById(Long id) {
		Discount discount = discountRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));

		return MenuMapper.toDto(discount);
	}

	public DiscountDto create(DiscountDto dto) {
		// TODO:
		Discount entity = MenuMapper.toEntity(dto);
		Discount saved = discountRepository.save(entity);
		return MenuMapper.toDto(saved);
	}

	public DiscountDto update(Long id, DiscountDto dto) {
		Discount existing = discountRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));
		existing.setName(dto.getName());
		existing.setDiscountType(dto.getDiscountType());
		existing.setAmount(dto.getAmount());
		existing.setAppliesTo(dto.getAppliesTo());
		existing.setReferenceId(dto.getReferenceId());
		existing.setStartDate(dto.getStartDate());
		existing.setEndDate(dto.getEndDate());
		existing.setIsActive(dto.getIsActive());
		Discount updated = discountRepository.save(existing);
		return MenuMapper.toDto(updated);
	}

	public void delete(Long id) {
		if (!discountRepository.existsById(id)) {
			throw new ResourceNotFoundException("Discount not found with id: " + id);
		}
		discountRepository.deleteById(id);
	}
}