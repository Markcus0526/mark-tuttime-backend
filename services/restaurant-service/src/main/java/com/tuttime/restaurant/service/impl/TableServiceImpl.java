package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.TableRequest;
import com.tuttime.restaurant.dto.TableResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.TableRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import com.tuttime.restaurant.service.RestaurantStaffService;
import com.tuttime.restaurant.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

	private final RestaurantStaffService restaurantStaffService;
	private final BranchRepository branchRepository;
	private final ZoneRepository zoneRepository;
	private final TableRepository tableRepository;

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
	public TableResponse getTableById(UUID tableId) {
		Table table = tableRepository.findByTableIdAndIsDeletedFalse(tableId)
				.orElseThrow(() -> new ResourceNotFoundException("Table not found in zone"));

		return mapToResponse(table);
	}

	@Override
	@Transactional
	public TableResponse createTable(TableRequest request, UUID currentUserId, String userRoles) {
		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(request.getZoneId())
				.orElseThrow(() -> new com.tuttime.common.exception.ResourceNotFoundException("Zone not found: " + request.getZoneId()));

		Branch branch = branchRepository.findByBranchIdAndIsDeletedFalse(zone.getBranchId())
				.orElseThrow(() -> new com.tuttime.common.exception.ResourceNotFoundException("Branch not found: " + zone.getBranchId()));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
			if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		// Optional: enforce unique table number within zone
		boolean exists = tableRepository.existsByZoneIdAndNumberAndIsDeletedFalse(zone.getZoneId(), request.getNumber());
		if (exists) {
			throw new IllegalArgumentException("Table number already exists in this zone");
		}

		Table table = new Table();
		table.setZoneId(zone.getZoneId());
		table.setNumber(request.getNumber());
		table.setSeats(request.getSeats());
		table.setStatus(request.getStatus());
		table.setIsClosed(request.getIsClosed() != null ? request.getIsClosed() : false);
		table.setDeleted(false);

		Table saved = tableRepository.save(table);
		return mapToResponse(saved);
	}

	@Override
	@Transactional
	public TableResponse updateTable(UUID tableId, TableRequest request, UUID currentUserId, String userRoles) {
		Table table = tableRepository.findByTableIdAndIsDeletedFalse(tableId)
				.orElseThrow(() -> new ResourceNotFoundException("Table not found"));

		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(table.getZoneId())
				.orElseThrow(() -> new com.tuttime.common.exception.ResourceNotFoundException("Zone not found: " + table.getZoneId()));

		Branch branch = branchRepository.findByBranchIdAndIsDeletedFalse(zone.getBranchId())
				.orElseThrow(() -> new com.tuttime.common.exception.ResourceNotFoundException("Branch not found: " + zone.getBranchId()));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
			if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		// Optional: if number changes, check uniqueness
		if (!table.getNumber().equals(request.getNumber())) {
			boolean exists = tableRepository.existsByZoneIdAndNumberAndIsDeletedFalse(zone.getZoneId(), request.getNumber());
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
	public void deleteTable(UUID tableId, UUID currentUserId, String userRoles) {
		Table table = tableRepository.findByTableIdAndIsDeletedFalse(tableId)
				.orElseThrow(() -> new ResourceNotFoundException("Table not found"));

		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(table.getZoneId())
				.orElseThrow(() -> new com.tuttime.common.exception.ResourceNotFoundException("Zone not found: " + table.getZoneId()));

		Branch branch = branchRepository.findByBranchIdAndIsDeletedFalse(zone.getBranchId())
				.orElseThrow(() -> new com.tuttime.common.exception.ResourceNotFoundException("Branch not found: " + zone.getBranchId()));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
			if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		// Soft delete
		table.setDeleted(true);
		table.setDeletedAt(OffsetDateTime.now());
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
	public void confirmReservation(UUID tableId, UUID reservationId, UUID currentUserId, String userRoles) {
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