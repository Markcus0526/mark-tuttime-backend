package com.tuttime.menu.controller;

import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.service.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus/{menuId}/menu-items")
@RequiredArgsConstructor
@Tag(name = "Menu Items", description = "Operations related to menu items")
public class MenuItemController {

	private final MenuItemService service;

	@PostMapping
	@Operation(summary = "Create a new menu item")
	public ResponseEntity<MenuItemDto> create(@RequestBody MenuItemDto dto) {
		return ResponseEntity.ok(service.create(dto));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get menu item by ID")
	public ResponseEntity<MenuItemDto> getById(@PathVariable Long id) {
		return ResponseEntity.ok(service.getById(id));
	}

	@GetMapping()
	@Operation(summary = "Get all menu items in a category")
	public ResponseEntity<List<MenuItemDto>> getByCategory(@PathVariable Long categoryId) {
		return ResponseEntity.ok(service.getByCategoryId(categoryId));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update menu item by ID")
	public ResponseEntity<MenuItemDto> update(@PathVariable Long id, @RequestBody MenuItemDto dto) {
		return ResponseEntity.ok(service.update(id, dto));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete menu item by ID")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}