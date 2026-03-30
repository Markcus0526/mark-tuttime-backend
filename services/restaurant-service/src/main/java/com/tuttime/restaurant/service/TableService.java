package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.TableRequest;
import com.tuttime.restaurant.dto.TableResponse;
import com.tuttime.restaurant.enums.TableStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TableService {

	List<TableResponse> getTablesByZoneId(UUID zoneId);

	TableResponse getTableById(UUID tableId, UUID zoneId);

	TableResponse createTable(UUID zoneId, TableRequest request);

	TableResponse updateTable(UUID tableId, UUID zoneId, TableRequest request);

	void deleteTable(UUID tableId, UUID zoneId);

	List<TableResponse> findAvailableTables(UUID zoneId, Instant from, Instant to);

	TableStatus getTableCurrentStatus(UUID tableId);

	void confirmReservation(UUID tableId, UUID reservationId);
}
