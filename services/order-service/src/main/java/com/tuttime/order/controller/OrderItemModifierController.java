package com.tuttime.order.controller;

import com.tuttime.order.dto.OrderItemModifierDTO;
import com.tuttime.order.service.OrderItemModifierService;
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
@RequestMapping("/api/v1/order-item-modifiers")
@RequiredArgsConstructor
@Tag(name = "Order Item Modifiers", description = "APIs for managing order item modifiers")
public class OrderItemModifierController {

	private final OrderItemModifierService modifierService;

	// Create a new modifier
	@Operation(summary = "Create a new modifier")
	@PostMapping
	public ResponseEntity<OrderItemModifierDTO> createModifier(@RequestBody OrderItemModifierDTO modifierDTO) {
		OrderItemModifierDTO createdModifier = modifierService.createModifier(modifierDTO);
		return new ResponseEntity<>(createdModifier, HttpStatus.CREATED);
	}

	// Update an existing modifier by ID
	@PutMapping("/{modifierId}")
	@Operation(summary = "Update an existing modifier by ID")
	public ResponseEntity<OrderItemModifierDTO> updateModifier(
			@Parameter(description = "ID of the modifier to update") @PathVariable UUID modifierId,
			@RequestBody OrderItemModifierDTO modifierDTO) {
		OrderItemModifierDTO updatedModifier = modifierService.updateModifier(modifierId, modifierDTO);
		return ResponseEntity.ok(updatedModifier);
	}

	// Get modifier by ID
	@Operation(summary = "Get modifier by ID")
	@GetMapping("/{modifierId}")
	public ResponseEntity<OrderItemModifierDTO> getModifierById(
			@Parameter(description = "ID of the modifier to retrieve") @PathVariable UUID modifierId) {
		OrderItemModifierDTO modifier = modifierService.getModifierById(modifierId);
		return ResponseEntity.ok(modifier);
	}

	// Get all modifiers for a specific order item by order item ID
	@Operation(summary = "Get all modifiers for a specific order item by order item ID")
	@GetMapping("/order-item/{orderItemId}")
	public ResponseEntity<List<OrderItemModifierDTO>> getModifiersByOrderItemId(
			@Parameter(description = "Order item ID to filter modifiers") @PathVariable UUID orderItemId) {
		List<OrderItemModifierDTO> modifiers = modifierService.getModifiersByOrderItemId(orderItemId);
		return ResponseEntity.ok(modifiers);
	}

	// Delete modifier by ID
	@Operation(summary = "Delete modifier by ID")
	@DeleteMapping("/{modifierId}")
	public ResponseEntity<Void> deleteModifier(
			@Parameter(description = "ID of the modifier to delete") @PathVariable UUID modifierId) {
		modifierService.deleteModifier(modifierId);
		return ResponseEntity.noContent().build();
	}
}