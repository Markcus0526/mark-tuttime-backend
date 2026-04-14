package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.DishTable;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.mapper.TableMapper;
import com.tuttime.restaurant.repository.TableRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.StaffService;
import com.tuttime.restaurant.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableServiceImpl implements TableService {

	private final StaffService staffService;
	private final ZoneRepository zoneRepository;
	private final TableRepository tableRepository;
	private final MediaService mediaService;
	private final TableMapper tableMapper;

	@Override
	public List<TableDto.TableResponse> getTablesByZoneId(UUID zoneId) {
		List<DishTable> tables = tableRepository.findAllByZoneIdAndDeletedFalse(zoneId);
		return tables.stream().map(tableMapper::toResponse).collect(Collectors.toList());
	}

	@Override
	public TableDto.TableResponse getTableById(UUID tableId) {
		DishTable dishTable = tableRepository.findByIdAndDeletedFalse(tableId)
				.orElseThrow(() -> new MethodArgumentNotValidException("Table not found in zone"));

		return tableMapper.toResponse(dishTable);
	}

	@Override
	@Transactional
	public TableDto.TableResponse createTable(TableDto.TableRequest request, UUID currentUserId, String userRoles) {
		Zone zone = zoneRepository.findByIdAndDeletedFalse(request.zoneId())
				.orElseThrow(() -> new MethodArgumentNotValidException("Zone not found: " + request.zoneId()));

		staffService.validatePermissions(zone.getBranch().getId(), currentUserId, userRoles);

		// Optional: enforce unique table number within zone
		boolean exists = tableRepository.existsByZoneIdAndNumberAndDeletedFalse(zone.getId(), request.number());
		if (exists) {
			throw new ConflictException("Table number already exists in this zone");
		}

		DishTable dishTable = new DishTable();
		dishTable.setZone(zone);
		dishTable.setNumber(request.number());
		dishTable.setSeats(request.seats());
		dishTable.setStatus(request.status());
		dishTable.setClosed(request.closed() != null ? request.closed() : false);
		dishTable.setDeleted(false);

		DishTable saved = tableRepository.save(dishTable);
		return tableMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public TableDto.TableResponse updateTable(UUID tableId, TableDto.TableRequest request, UUID currentUserId, String userRoles) {
		DishTable dishTable = tableRepository.findByIdAndDeletedFalse(tableId)
				.orElseThrow(() -> new MethodArgumentNotValidException("Table not found"));

		Zone zone = zoneRepository.findByIdAndDeletedFalse(dishTable.getZone().getId())
				.orElseThrow(() -> new MethodArgumentNotValidException("Zone not found: " + dishTable.getZone().getId()));

		staffService.validatePermissions(zone.getBranch().getId(), currentUserId, userRoles);

		// Optional: if number changes, check uniqueness
		if (!dishTable.getNumber().equals(request.number())) {
			boolean exists = tableRepository.existsByZoneIdAndNumberAndDeletedFalse(zone.getId(), request.number());
			if (exists) {
				throw new ConflictException("Table number already exists in this zone");
			}
			dishTable.setNumber(request.number());
		}

		tableMapper.updateEntityFromRequest(request, dishTable);

		// No need for .save(dishTable) - Hibernate dirty checking handles it
		return tableMapper.toResponse(dishTable);
	}

	@Override
	@Transactional
	public void deleteTable(UUID tableId, UUID currentUserId, String userRoles) {
		DishTable dishTable = tableRepository.findByIdAndDeletedFalse(tableId)
				.orElseThrow(() -> new MethodArgumentNotValidException("Table not found"));

		Zone zone = zoneRepository.findByIdAndDeletedFalse(dishTable.getZone().getId())
				.orElseThrow(() -> new MethodArgumentNotValidException("Zone not found: " + dishTable.getZone().getId()));

		staffService.validatePermissions(zone.getBranch().getId(), currentUserId, userRoles);

		// Soft delete
		dishTable.setDeleted(true);
		tableRepository.save(dishTable);
	}

	@Override
	public List<TableDto.TableResponse> findAvailableTables(UUID zoneId, OffsetDateTime from, OffsetDateTime to) {
		List<DishTable> dishTables = tableRepository.findAllByZoneIdAndDeletedFalseAndClosedFalse(zoneId);

		return dishTables.stream()
				.filter(t -> t.getStatus() == TableStatus.ACTIVE)
				.map(tableMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	public TableStatus getTableCurrentStatus(UUID tableId) {
		return tableRepository.findByIdAndDeletedFalse(tableId)
				.map(DishTable::getStatus) // Extracts status if table exists
				.orElse(TableStatus.CLOSED); // Or throw an exception
	}

	@Override
	@Transactional
	public boolean deleteAllByZoneId(UUID zoneId) {
		List<DishTable> dishTables = tableRepository.findAllByZoneIdAndDeletedFalse(zoneId);
		if (dishTables.isEmpty()) {
			return true;
		}

		for (DishTable dishTable : dishTables) {
			mediaService.deleteAllByEntityId(dishTable.getId());
		}

		tableRepository.deleteAllByZoneId(zoneId);
		return true;
	}
}