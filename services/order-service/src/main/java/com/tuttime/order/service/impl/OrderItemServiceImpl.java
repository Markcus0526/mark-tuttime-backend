package com.tuttime.order.service.impl;

import com.tuttime.order.dto.OrderItemDTO;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.mapper.OrderItemMapper;
import com.tuttime.order.repository.OrderItemRepository;
import com.tuttime.order.service.OrderItemService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderItemServiceImpl implements OrderItemService {

	private final OrderItemRepository orderItemRepository;
	private final OrderItemMapper orderItemMapper;

	public OrderItemServiceImpl(OrderItemRepository orderItemRepository,
	                            OrderItemMapper orderItemMapper) {
		this.orderItemRepository = orderItemRepository;
		this.orderItemMapper = orderItemMapper;
	}

	@Override
	public OrderItemDTO createOrderItem(OrderItemDTO orderItemDTO) {
		OrderItem orderItem = orderItemMapper.toEntity(orderItemDTO);
		OrderItem saved = orderItemRepository.save(orderItem);
		return orderItemMapper.toDto(saved);
	}

	@Override
	public OrderItemDTO updateOrderItem(UUID orderItemId, OrderItemDTO orderItemDTO) {
		OrderItem existing = orderItemRepository.findById(orderItemId)
				.orElseThrow(() -> new IllegalArgumentException("OrderItem not found: " + orderItemId));

		existing.setMenuItemId(orderItemDTO.getMenuItemId());
		existing.setQuantity(orderItemDTO.getQuantity());
		existing.setPrice(orderItemDTO.getPrice());

		// Handle modifiers update if necessary (requires extra logic)

		OrderItem updated = orderItemRepository.save(existing);
		return orderItemMapper.toDto(updated);
	}

	@Override
	public OrderItemDTO getOrderItemById(UUID orderItemId) {
		OrderItem orderItem = orderItemRepository.findById(orderItemId)
				.orElseThrow(() -> new IllegalArgumentException("OrderItem not found: " + orderItemId));
		return orderItemMapper.toDto(orderItem);
	}

	@Override
	public List<OrderItemDTO> getItemsByOrderId(UUID orderId) {
		List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
		return items.stream().map(orderItemMapper::toDto).collect(Collectors.toList());
	}

	@Override
	public void deleteOrderItem(UUID orderItemId) {
		if (!orderItemRepository.existsById(orderItemId)) {
			throw new IllegalArgumentException("OrderItem not found: " + orderItemId);
		}
		orderItemRepository.deleteById(orderItemId);
	}
}

