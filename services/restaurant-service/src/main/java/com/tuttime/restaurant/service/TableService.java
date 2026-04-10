package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.TableRequest;
import com.tuttime.restaurant.dto.TableResponse;
import com.tuttime.restaurant.entity.enums.TableStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TableService {

	List<TableResponse> getTablesByZoneId(UUID zoneId);

	TableResponse getTableById(UUID tableId);

	TableResponse createTable(TableRequest request, UUID currentUserId, String userRoles);

	TableResponse updateTable(UUID tableId, TableRequest request, UUID currentUserId, String userRoles);

	void deleteTable(UUID tableId, UUID currentUserId, String userRoles);

	List<TableResponse> findAvailableTables(UUID zoneId, OffsetDateTime from, OffsetDateTime to);

	TableStatus getTableCurrentStatus(UUID tableId);

	void confirmReservation(UUID tableId, UUID reservationId, UUID currentUserId, String userRoles);
}
