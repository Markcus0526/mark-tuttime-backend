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
		Zone zone = zoneRepository.findByIdAndDeletedFalse(zoneId)
				.orElseThrow(() -> new InvalidRequestException("Zone not found with id " + zoneId));

		staffService.validatePermissions(zone.getBranch().getId(), currentUserId, userRoles);

		// 1. Sync Media
		Media media = mediaRepository.findByEntityIdAndMediaType(zoneId, MediaType.CANVAS)
				.orElseGet(() -> {
					Media m = new Media();
					m.setEntityId(zoneId);
					m.setMediaType(MediaType.CANVAS);
					return m;
				});
		media.setLayoutConfig(Map.of("canvas", dto.canvas(), "fixtures", dto.fixtures()));
		mediaRepository.save(media);

		// 2. Identify Deletions
		List<DishTable> existingInDb = tableRepository.findAllByZoneIdAndDeletedFalse(zoneId);
		Set<UUID> incomingIds = dto.tables().stream()
				.map(ZoneLayoutDto.TableDto::id)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		existingInDb.stream()
				.filter(t -> !incomingIds.contains(t.getId()))
				.forEach(t -> {
					if (t.getStatus() != TableStatus.ACTIVE)
						throw new IllegalStateException("Table " + t.getLabel() + " is busy!");
					t.setDeleted(true);
					tableRepository.save(t);
				});

		// IMPORTANT: Force the deletions to happen in the DB NOW
		// before we try to insert/update new labels.
		tableRepository.flush();

		// 3. Upsert Tables
		for (ZoneLayoutDto.TableDto tDto : dto.tables()) {
			// If ID is null or not found, it's a new table
			DishTable table = (tDto.id() != null)
					? tableRepository.findById(tDto.id()).orElse(new DishTable())
					: new DishTable();

			if (tDto.id() != null) {
				table.setId(tDto.id());
			}

			table.setZone(zone);
			table.setLabel(tDto.label());
			table.setNotes(tDto.notes());
			table.setSeats(tDto.seats());
			table.setPosX(tDto.x());
			table.setPosY(tDto.y());
			table.setTableType(tDto.type());
			table.setDeleted(false);
			tableRepository.save(table);
		}
	}

}
