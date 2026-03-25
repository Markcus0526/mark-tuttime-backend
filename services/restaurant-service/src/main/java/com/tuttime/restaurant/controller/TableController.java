package com.tuttime.restaurant.controller;

import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.mapper.TableMapper;
import com.tuttime.restaurant.service.TableService;
import com.tuttime.restaurant.service.ZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
	public ResponseEntity<TableDto> getById(@PathVariable Long id) {
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
	public ResponseEntity<TableDto> update(@PathVariable Long id, @RequestBody TableDto dto) {
		Zone zone = zoneService.getById(dto.getZoneId());
		Table table = tableMapper.toEntity(dto, zone);
		Table updated = tableService.update(id, table);
		return ResponseEntity.ok(tableMapper.toDto(updated));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a table by ID")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		tableService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
