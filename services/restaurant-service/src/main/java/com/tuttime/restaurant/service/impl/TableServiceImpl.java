package com.tuttime.restaurant.service.impl;

import com.tuttime.restaurant.dto.TableRequest;
import com.tuttime.restaurant.dto.TableResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.TableRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import com.tuttime.restaurant.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableServiceImpl implements TableService {

	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final ZoneRepository zoneRepository;
	private final TableRepository tableRepository;

	// Helper method to validate full hierarchy exists and is not soft-deleted
	private Zone validateHierarchy(UUID restaurantId, UUID branchId, UUID zoneId) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

		Branch branch = branchRepository.findByBranchIdAndRestaurantIdAndIsDeletedFalse(branchId, restaurant.getRestaurantId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found in restaurant"));

		Zone zone = zoneRepository.findByZoneIdAndBranchIdAndIsDeletedFalse(zoneId, branch.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found in branch"));

		return zone;
	}

	@Override
	public List<TableResponse> getTablesByZoneId(UUID zoneId) {
		// Validate zone exists and is active (optional: check hierarchy if needed)
		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found"));

		return tableRepository.findAllByZoneIdAndIsDeletedFalse(zoneId).stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	@Override
	public TableResponse getTableById(UUID tableId, UUID zoneId) {
		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found"));

		Table table = tableRepository.findByTableIdAndZoneIdAndIsDeletedFalse(tableId, zone.getZoneId())
				.orElseThrow(() -> new ResourceNotFoundException("Table not found in zone"));

		return mapToResponse(table);
	}

	@Override
	@Transactional
	public TableResponse createTable(UUID zoneId, TableRequest request) {
		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found"));

		// Optional: enforce unique table number within zone
		boolean exists = tableRepository.existsByZoneIdAndNumberAndIsDeletedFalse(zoneId, request.getNumber());
		if (exists) {
			throw new IllegalArgumentException("Table number already exists in this zone");
		}

		Table table = new Table();
		table.setZoneId(zone.getZoneId());
		table.setNumber(request.getNumber());
		table.setSeats(request.getSeats());
		table.setStatus(request.getStatus());
		table.setIsClosed(request.getIsClosed() != null ? request.getIsClosed() : false);
		table.setIsDeleted(false);

		Table saved = tableRepository.save(table);
		return mapToResponse(saved);
	}

	@Override
	@Transactional
	public TableResponse updateTable(UUID tableId, UUID zoneId, TableRequest request) {
		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found"));

		Table table = tableRepository.findByTableIdAndZoneIdAndIsDeletedFalse(tableId, zone.getZoneId())
				.orElseThrow(() -> new ResourceNotFoundException("Table not found in zone"));

		// Optional: if number changes, check uniqueness
		if (!table.getNumber().equals(request.getNumber())) {
			boolean exists = tableRepository.existsByZoneIdAndNumberAndIsDeletedFalse(zoneId, request.getNumber());
			if (exists) {
				throw new IllegalArgumentException("Table number already exists in this zone");
			}
			table.setNumber(request.getNumber());
		}

		table.setSeats(request.getSeats());
		table.setStatus(request.getStatus());
		table.setIsClosed(request.getIsClosed() != null ? request.getIsClosed() : table.getIsClosed());

		Table updated = tableRepository.save(table);
		return mapToResponse(updated);
	}

	@Override
	@Transactional
	public void deleteTable(UUID tableId, UUID zoneId) {
		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found"));

		Table table = tableRepository.findByTableIdAndZoneIdAndIsDeletedFalse(tableId, zone.getZoneId())
				.orElseThrow(() -> new ResourceNotFoundException("Table not found in zone"));

		// Soft delete
		table.setIsDeleted(true);
		tableRepository.save(table);
	}

	@Override
	public List<TableResponse> findAvailableTables(UUID zoneId, OffsetDateTime from, OffsetDateTime to) {
		List<Table> tables = tableRepository.findByZoneIdAndIsDeletedFalseAndIsClosedFalse(zoneId);

		return tables.stream()
				.filter(t -> t.getStatus() == TableStatus.ACTIVE)
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	@Override
	public TableStatus getTableCurrentStatus(UUID tableId) {
		return tableRepository.findByTableIdAndIsDeletedFalse(tableId)
				.map(Table::getStatus) // Extracts status if table exists
				.orElse(TableStatus.CLOSED); // Or throw an exception
	}

	@Override
	public void confirmReservation(UUID tableId, UUID reservationId) {
		// This might be optional, e.g., store linkage or update local cache/state
		// Possibly call Booking Service to confirm or just log it locally
		// For example:
		// bookingServiceClient.confirmReservation(reservationId, tableId);
	}

	private TableResponse mapToResponse(Table table) {
		return TableResponse.builder()
				.tableId(table.getTableId())
				.zoneId(table.getZoneId())
				.number(table.getNumber())
				.seats(table.getSeats())
				.status(table.getStatus())
				.isClosed(table.getIsClosed())
				.createdAt(table.getCreatedAt())
				.updatedAt(table.getUpdatedAt())
				.build();
	}
}