package com.tuttime.restaurant.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.mapper.TableMapper;
import com.tuttime.restaurant.service.TableService;
import com.tuttime.restaurant.service.ZoneService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/tables")
@Tag(name = "Tables", description = "Operations related to menu tables")
public class TableController {

	private final TableService tableService;
	private final ZoneService zoneService;
	private final TableMapper tableMapper;

	public TableController(TableService tableService, ZoneService zoneService, TableMapper tableMapper) {
		this.tableService = tableService;
		this.zoneService = zoneService;
		this.tableMapper = tableMapper;
	}

	@GetMapping
	@Operation(summary = "Get all tables")
	public List<TableDto> getAll() {
		return tableService.getAll()
				.stream()
				.map(tableMapper::toDto)
				.collect(Collectors.toList());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a table by ID")
	public ResponseEntity<TableDto> getById(@PathVariable UUID id) {
		Table table = tableService.getById(id);
		return ResponseEntity.ok(tableMapper.toDto(table));
	}

	@PostMapping
	@Operation(summary = "Create a new table")
	public ResponseEntity<TableDto> create(@RequestBody TableDto dto) {
		Zone zone = zoneService.getById(dto.getZoneId());
		Table table = tableMapper.toEntity(dto, zone);
		Table saved = tableService.create(table);
		return ResponseEntity.ok(tableMapper.toDto(saved));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing table")
	public ResponseEntity<TableDto> update(@PathVariable UUID id, @RequestBody TableDto dto) {
		Zone zone = zoneService.getById(dto.getZoneId());
		Table table = tableMapper.toEntity(dto, zone);
		Table updated = tableService.update(id, table);
		return ResponseEntity.ok(tableMapper.toDto(updated));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a table by ID")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		tableService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
