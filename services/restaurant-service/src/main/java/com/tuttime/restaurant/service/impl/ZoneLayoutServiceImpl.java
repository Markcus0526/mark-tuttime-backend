package com.tuttime.restaurant.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.dto.ZoneLayoutDto;
import com.tuttime.restaurant.entity.DishTable;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.entity.enums.MediaType;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.repository.MediaRepository;
import com.tuttime.restaurant.repository.TableRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import com.tuttime.restaurant.service.StaffService;
import com.tuttime.restaurant.service.TableService;
import com.tuttime.restaurant.service.ZoneLayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneLayoutServiceImpl implements ZoneLayoutService {

	private final StaffService staffService;
	private final ZoneRepository zoneRepository;
	private final TableRepository tableRepository;
	private final MediaRepository mediaRepository;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public ZoneLayoutDto.LayoutDto getZoneLayout(UUID zoneId) {
		Zone zone = zoneRepository.findByIdAndDeletedFalse(zoneId)
				.orElseThrow(() -> new InvalidRequestException("Zone not found with id " + zoneId));

		// 1. Fetch the Media (Canvas & Fixtures)
		Media media = mediaRepository.findByEntityIdAndMediaType(zoneId, MediaType.CANVAS)
				.orElseThrow(() -> new InvalidRequestException("Layout configuration not found for zone: " + zoneId));

		// 2. Fetch all non-deleted Tables in this Zone
		List<DishTable> tableEntities = tableRepository.findAllByZoneIdAndDeletedFalse(zoneId);

		// 3. Extract Canvas and Fixtures from JSONB
		Map<String, Object> config = media.getLayoutConfig();

		// Map the nested JSON objects back to DTOs using Jackson
		ZoneLayoutDto.CanvasDto canvas = objectMapper.convertValue(config.get("canvas"), ZoneLayoutDto.CanvasDto.class);

		List<ZoneLayoutDto.FixtureDto> fixtures = objectMapper.convertValue(
				config.get("fixtures"),
				new TypeReference<List<ZoneLayoutDto.FixtureDto>>() {}
		);

		// 4. Map TableEntities to DTOs (Merging DB status/coords with UI format)
		List<ZoneLayoutDto.TableDto> tables = tableEntities.stream()
				.map(t -> new ZoneLayoutDto.TableDto(
						t.getId(),
						t.getTableType(),
						t.getPosX(),
						t.getPosY(),
						t.getStatus().name(),
						t.getLabel(), // label
						t.getNotes(),
						t.getSeats()
				))
				.toList();

		return new ZoneLayoutDto.LayoutDto(canvas, tables, fixtures);
	}

	@Transactional
	public void syncLayout(UUID zoneId, ZoneLayoutDto.LayoutDto dto, UUID currentUserId, String userRoles) {
		// 1. Validate Zone and Permissions
		Zone zone = zoneRepository.findByIdAndDeletedFalse(zoneId)
				.orElseThrow(() -> new InvalidRequestException("Zone not found with id " + zoneId));

		staffService.validatePermissions(zone.getBranch().getId(), currentUserId, userRoles);

		// 2. Sync Media (Using HashMap to allow nulls and prevent Map.of NPE)
		Media media = mediaRepository.findByEntityIdAndMediaType(zoneId, MediaType.CANVAS)
				.orElseGet(() -> {
					Media m = new Media();
					m.setEntityId(zoneId);
					m.setMediaType(MediaType.CANVAS);
					return m;
				});

		Map<String, Object> config = new HashMap<>();
		config.put("canvas", dto.canvas());
		config.put("fixtures", dto.fixtures());
		media.setLayoutConfig(config);
		mediaRepository.save(media);

		// 3. Identify Deletions
		List<DishTable> existingInDb = tableRepository.findAllByZoneIdAndDeletedFalse(zoneId);

		// Create a set of incoming IDs for O(1) lookup
		Set<UUID> incomingIds = dto.tables().stream()
				.map(ZoneLayoutDto.TableDto::id)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<DishTable> toDelete = existingInDb.stream()
				.filter(t -> !incomingIds.contains(t.getId()))
				.peek(t -> {
					if (t.getStatus() != TableStatus.ACTIVE) {
						throw new InvalidRequestException("Table " + t.getLabel() + " is currently busy and cannot be removed!");
					}
					t.setDeleted(true);
				})
				.collect(Collectors.toList());

		if (!toDelete.isEmpty()) {
			tableRepository.saveAll(toDelete);
			tableRepository.flush(); // Clear deleted labels from DB constraints before upserting
		}

		// 4. Batch Upsert Tables
		List<DishTable> toSave = new ArrayList<>();
		for (ZoneLayoutDto.TableDto tDto : dto.tables()) {
			DishTable table = (tDto.id() != null)
					? tableRepository.findById(tDto.id()).orElseGet(DishTable::new)
					: new DishTable();

			table.setZone(zone);
			table.setLabel(tDto.label());
			table.setNotes(tDto.notes());
			table.setSeats(tDto.seats());
			table.setPosX(tDto.x());
			table.setPosY(tDto.y());
			table.setTableType(tDto.type());
			table.setDeleted(false);

			toSave.add(table);
		}

		tableRepository.saveAll(toSave);
	}

}
