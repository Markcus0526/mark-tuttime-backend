package com.tuttime.order.service.impl;

import com.tuttime.order.dto.OrderDto;
import com.tuttime.order.entity.Order;
import com.tuttime.order.enums.OrderStatus;
import com.tuttime.order.mapper.OrderMapper;
import com.tuttime.order.repository.OrderRepository;
import com.tuttime.order.service.OrderService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;

	public OrderServiceImpl(OrderRepository orderRepository,
	                        OrderMapper orderMapper) {
		this.orderRepository = orderRepository;
		this.orderMapper = orderMapper;
	}

	@Override
	public OrderDto createOrder(OrderDto orderDto) {
		Order order = orderMapper.toEntity(orderDto);
		order.setStatus(OrderStatus.CREATED);
		// Additional business logic here (e.g., validate booking exists)
		Order saved = orderRepository.save(order);
		return orderMapper.toDto(saved);
	}

	@Override
	public OrderDto updateOrder(UUID orderId, OrderDto orderDto) {
		Order existingOrder = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

		// Basic validation
		if (orderDto.getTotalAmount() == null || orderDto.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Total amount must be non-negative");
		}

		// Update fields
		existingOrder.setTotalAmount(orderDto.getTotalAmount());
		existingOrder.setBookingId(orderDto.getBookingId());
		existingOrder.setUserId(orderDto.getUserId());

		// Update items with mapper context
		if (orderDto.getOrderItems() != null && !orderDto.getOrderItems().isEmpty()) {
			existingOrder.getOrderItems().clear();
			Order tempOrder = orderMapper.toEntity(orderDto);
			existingOrder.getOrderItems().addAll(tempOrder.getOrderItems());
			// Mapper handles backrefs
		}

		Order updated = orderRepository.save(existingOrder);
		return orderMapper.toDto(updated);
	}

	@Override
	public OrderDto getOrderById(UUID orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
		return orderMapper.toDto(order);
	}

	@Override
	public List<OrderDto> getOrdersByBookingId(UUID bookingId) {
		List<Order> orders = orderRepository.findByBookingId(bookingId);
		return orders.stream().map(orderMapper::toDto).collect(Collectors.toList());
	}

	@Override
	public List<OrderDto> getOrdersByUserId(UUID userId) {
		List<Order> orders = orderRepository.findByUserId(userId);
		return orders.stream().map(orderMapper::toDto).collect(Collectors.toList());
	}

	@Override
	public void deleteOrder(UUID orderId) {
		if (!orderRepository.existsById(orderId)) {
			throw new IllegalArgumentException("Order not found: " + orderId);
		}
		orderRepository.deleteById(orderId);
	}

	@Override
	public OrderDto updateOrderStatus(UUID orderId, String status) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
		if (!StringUtils.hasText(status)) {
			throw new IllegalArgumentException("Status must not be empty");
		}
		OrderStatus orderStatus;
		try {
			orderStatus = OrderStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid order status: " + status);
		}
		order.setStatus(orderStatus);
		Order updated = orderRepository.save(order);
		return orderMapper.toDto(updated);
	}
}
