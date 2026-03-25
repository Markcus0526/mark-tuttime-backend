package com.tuttime.order.controller;

import com.tuttime.order.dto.OrderItemDTO;
import com.tuttime.order.service.OrderItemService;
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
@RequestMapping("/api/v1/order-items")
@RequiredArgsConstructor
@Tag(name = "Order Items", description = "APIs for managing order items")
public class OrderItemController {

	private final OrderItemService orderItemService;

	// Create a new order item
	@Operation(summary = "Create a new order item")
	@PostMapping
	public ResponseEntity<OrderItemDTO> createOrderItem(@RequestBody OrderItemDTO orderItemDTO) {
		OrderItemDTO createdItem = orderItemService.createOrderItem(orderItemDTO);
		return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
	}

	// Update an existing order item by ID
	@Operation(summary = "Update an existing order item by ID")
	@PutMapping("/{orderItemId}")
	public ResponseEntity<OrderItemDTO> updateOrderItem(
			@Parameter(description = "ID of the order item to update") @PathVariable UUID orderItemId,
			@RequestBody OrderItemDTO orderItemDTO) {
		OrderItemDTO updatedItem = orderItemService.updateOrderItem(orderItemId, orderItemDTO);
		return ResponseEntity.ok(updatedItem);
	}

	// Get order item by ID
	@Operation(summary = "Get order item by ID")
	@GetMapping("/{orderItemId}")
	public ResponseEntity<OrderItemDTO> getOrderItemById(
			@Parameter(description = "ID of the order item to retrieve") @PathVariable UUID orderItemId) {
		OrderItemDTO item = orderItemService.getOrderItemById(orderItemId);
		return ResponseEntity.ok(item);
	}

	// Get all items for a specific order by order ID
	@Operation(summary = "Get all items for a specific order by order ID")
	@GetMapping("/order/{orderId}")
	public ResponseEntity<List<OrderItemDTO>> getItemsByOrderId(
			@Parameter(description = "Order ID to filter items") @PathVariable UUID orderId) {
		List<OrderItemDTO> items = orderItemService.getItemsByOrderId(orderId);
		return ResponseEntity.ok(items);
	}

	// Delete order item by ID
	@Operation(summary = "Delete order item by ID")
	@DeleteMapping("/{orderItemId}")
	public ResponseEntity<Void> deleteOrderItem(
			@Parameter(description = "ID of the order item to delete") @PathVariable UUID orderItemId) {
		orderItemService.deleteOrderItem(orderItemId);
		return ResponseEntity.noContent().build();
	}
}