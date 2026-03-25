package com.tuttime.menu.controller;

import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@Tag(name = "Discounts", description = "Operations related to discounts")
public class DiscountController {
	private final DiscountService discountService;

	@PostMapping
	@Operation(summary = "Create a new discount")
	public ResponseEntity<DiscountDto> create(@RequestBody DiscountDto discount) {
		return ResponseEntity.ok(discountService.create(discount));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get discount by ID")
	public ResponseEntity<DiscountDto> getById(@PathVariable Long id) {
		return ResponseEntity.ok(discountService.getById(id));
	}

	@GetMapping("/reference/{referenceId}")
	@Operation(summary = "Get discounts by reference ID")
	public ResponseEntity<List<DiscountDto>> getByReferenceId(@PathVariable Long referenceId) {
		return ResponseEntity.ok(discountService.getByReferenceId(referenceId));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update discount by ID")
	public ResponseEntity<DiscountDto> update(@PathVariable Long id, @RequestBody DiscountDto discount) {
		return ResponseEntity.ok(discountService.update(id, discount));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete discount by ID")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		discountService.delete(id);
		return ResponseEntity.noContent().build();
	}
}