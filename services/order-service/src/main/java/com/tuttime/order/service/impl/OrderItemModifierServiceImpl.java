package com.tuttime.order.service.impl;

import com.tuttime.order.dto.OrderItemModifierDto;
import com.tuttime.order.entity.OrderItemModifier;
import com.tuttime.order.mapper.OrderItemModifierMapper;
import com.tuttime.order.repository.OrderItemModifierRepository;
import com.tuttime.order.service.OrderItemModifierService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderItemModifierServiceImpl implements OrderItemModifierService {

	private final OrderItemModifierRepository modifierRepository;
	private final OrderItemModifierMapper modifierMapper;

	public OrderItemModifierServiceImpl(OrderItemModifierRepository modifierRepository,
	                                    OrderItemModifierMapper modifierMapper) {
		this.modifierRepository = modifierRepository;
		this.modifierMapper = modifierMapper;
	}

	@Override
	public OrderItemModifierDto createModifier(OrderItemModifierDto modifierDTO) {
		OrderItemModifier modifier = modifierMapper.toEntity(modifierDTO);
		OrderItemModifier saved = modifierRepository.save(modifier);
		return modifierMapper.toDto(saved);
	}

	@Override
	public OrderItemModifierDto updateModifier(UUID modifierId, OrderItemModifierDto modifierDTO) {
		OrderItemModifier existing = modifierRepository.findById(modifierId)
				.orElseThrow(() -> new IllegalArgumentException("Modifier not found: " + modifierId));

		existing.setModifierId(modifierDTO.getModifierId());
		existing.setPriceAdjustment(modifierDTO.getPriceAdjustment());

		OrderItemModifier updated = modifierRepository.save(existing);
		return modifierMapper.toDto(updated);
	}

	@Override
	public OrderItemModifierDto getModifierById(UUID modifierId) {
		OrderItemModifier modifier = modifierRepository.findById(modifierId)
				.orElseThrow(() -> new IllegalArgumentException("Modifier not found: " + modifierId));
		return modifierMapper.toDto(modifier);
	}

	@Override
	public List<OrderItemModifierDto> getModifiersByOrderItemId(UUID orderItemId) {
		List<OrderItemModifier> modifiers = modifierRepository.findByOrderItemId(orderItemId);
		return modifiers.stream().map(modifierMapper::toDto).collect(Collectors.toList());
	}

	@Override
	public void deleteModifier(UUID modifierId) {
		if (!modifierRepository.existsById(modifierId)) {
			throw new IllegalArgumentException("Modifier not found: " + modifierId);
		}
		modifierRepository.deleteById(modifierId);
	}
}
