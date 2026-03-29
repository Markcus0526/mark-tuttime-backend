package com.tuttime.restaurant.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.dto.TableRequest;
import com.tuttime.restaurant.dto.TableResponse;
import com.tuttime.restaurant.entity.Table;

public interface TableService {

	List<TableResponse> getTablesByZoneId(UUID zoneId);

	TableResponse getTableById(UUID tableId, UUID zoneId);

	TableResponse createTable(UUID zoneId, TableRequest request);

	TableResponse updateTable(UUID tableId, UUID zoneId, TableRequest request);

	void deleteTable(UUID tableId, UUID zoneId);
}
