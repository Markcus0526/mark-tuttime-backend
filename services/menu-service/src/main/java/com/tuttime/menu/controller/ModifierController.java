package com.tuttime.menu.controller;

import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.service.ModifierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus/modifiers")
@RequiredArgsConstructor
@Tag(name = "Modifiers", description = "Operations related to modifiers")
public class ModifierController {
	private final ModifierService service;

	@PostMapping
	@Operation(summary = "Create a new modifier")
	public ResponseEntity<ModifierDto> create(@RequestBody ModifierDto dto) {
		return ResponseEntity.ok(service.create(dto));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get modifier by ID")
	public ResponseEntity<ModifierDto> getById(@PathVariable Long id) {
		return ResponseEntity.ok(service.getById(id));
	}

	@GetMapping
	@Operation(summary = "Get all modifiers")
	public ResponseEntity<List<ModifierDto>> getAll() {
		return ResponseEntity.ok(service.getAll());
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update modifier by ID")
	public ResponseEntity<ModifierDto> update(@PathVariable Long id, @RequestBody ModifierDto dto) {
		return ResponseEntity.ok(service.update(id, dto));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete modifier by ID")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}