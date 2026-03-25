package com.tuttime.order.controller;

import com.tuttime.order.dto.OrderDTO;
import com.tuttime.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "APIs for managing orders")
public class OrderController {

	private final OrderService orderService;

	// Create a new order
	@Operation(summary = "Create a new order")
	@PostMapping
	public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
		OrderDTO createdOrder = orderService.createOrder(orderDTO);
		return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
	}

	// Update an existing order by ID
	@Operation(summary = "Update an existing order by ID")
	@PutMapping("/{orderId}")
	public ResponseEntity<OrderDTO> updateOrder(
			@Parameter(description = "ID of the order to update") @PathVariable UUID orderId,
			@RequestBody OrderDTO orderDTO) {
		OrderDTO updatedOrder = orderService.updateOrder(orderId, orderDTO);
		return ResponseEntity.ok(updatedOrder);
	}

	// Get order by ID
	@Operation(summary = "Get order by ID")
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderDTO> getOrderById(
			@Parameter(description = "ID of the order to retrieve") @PathVariable UUID orderId) {
		OrderDTO order = orderService.getOrderById(orderId);
		return ResponseEntity.ok(order);
	}

	// Get orders by booking ID
	@Operation(summary = "Get orders by booking ID")
	@GetMapping("/booking/{bookingId}")
	public ResponseEntity<List<OrderDTO>> getOrdersByBookingId(
			@Parameter(description = "Booking ID to filter orders") @PathVariable UUID bookingId) {
		List<OrderDTO> orders = orderService.getOrdersByBookingId(bookingId);
		return ResponseEntity.ok(orders);
	}

	// Get orders by user ID
	@Operation(summary = "Get orders by user ID")
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<OrderDTO>> getOrdersByUserId(
			@Parameter(description = "User ID to filter orders") @PathVariable UUID userId) {
		List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
		return ResponseEntity.ok(orders);
	}

	// Delete order by ID
	@Operation(summary = "Delete order by ID")
	@DeleteMapping("/{orderId}")
	public ResponseEntity<Void> deleteOrder(
			@Parameter(description = "ID of the order to delete") @PathVariable UUID orderId) {
		orderService.deleteOrder(orderId);
		return ResponseEntity.noContent().build();
	}

	// Update order status
	@Operation(summary = "Update order status")
	@PatchMapping("/{orderId}/status")
	public ResponseEntity<OrderDTO> updateOrderStatus(
			@Parameter(description = "ID of the order to update status") @PathVariable UUID orderId,
			@Parameter(description = "New status value") @RequestParam String status) {
		OrderDTO updatedOrder = orderService.updateOrderStatus(orderId, status);
		return ResponseEntity.ok(updatedOrder);
	}
}
