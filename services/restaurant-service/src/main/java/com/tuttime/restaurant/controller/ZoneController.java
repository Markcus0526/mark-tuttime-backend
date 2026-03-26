package com.tuttime.restaurant.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.mapper.ZoneMapper;
import com.tuttime.restaurant.service.BranchService;
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
@RequestMapping("/api/v1/zones")
@Tag(name = "Zones", description = "Operations related to menu zones")
public class ZoneController {

	private final ZoneService zoneService;
	private final BranchService branchService;
	private final ZoneMapper zoneMapper;

	public ZoneController(ZoneService zoneService, BranchService branchService, ZoneMapper zoneMapper) {
		this.zoneService = zoneService;
		this.branchService = branchService;
		this.zoneMapper = zoneMapper;
	}

	@GetMapping
	@Operation(summary = "Get all zones")
	public List<ZoneDto> getAll() {
		return zoneService.getAll()
				.stream()
				.map(zoneMapper::toDto)
				.collect(Collectors.toList());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a zone by ID")
	public ResponseEntity<ZoneDto> getById(@PathVariable UUID id) {
		Zone zone = zoneService.getById(id);
		return ResponseEntity.ok(zoneMapper.toDto(zone));
	}

	@PostMapping
	@Operation(summary = "Create a new zone")
	public ResponseEntity<ZoneDto> create(@RequestBody ZoneDto dto) {
		Branch branch = branchService.getById(dto.getBranchId());
		Zone zone = zoneMapper.toEntity(dto, branch);
		Zone saved = zoneService.create(zone);
		return ResponseEntity.ok(zoneMapper.toDto(saved));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing zone")
	public ResponseEntity<ZoneDto> update(@PathVariable UUID id, @RequestBody ZoneDto dto) {
		Branch branch = branchService.getById(dto.getBranchId());
		Zone zone = zoneMapper.toEntity(dto, branch);
		Zone updated = zoneService.update(id, zone);
		return ResponseEntity.ok(zoneMapper.toDto(updated));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a zone by ID")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		zoneService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
