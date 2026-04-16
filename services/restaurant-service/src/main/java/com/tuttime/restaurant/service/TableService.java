package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.enums.TableStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TableService {

	List<TableDto.TableResponse> getTablesByZoneId(UUID zoneId);

	TableDto.TableDetailedResponse getTableById(UUID tableId);

	TableDto.TableResponse createTable(TableDto.TableRequest request, UUID currentUserId, String userRoles);

	TableDto.TableResponse updateTable(UUID tableId, TableDto.TableRequest request, UUID currentUserId, String userRoles);

	void deleteTable(UUID tableId, UUID currentUserId, String userRoles);

	List<TableDto.TableResponse> findAvailableTables(UUID zoneId, OffsetDateTime from, OffsetDateTime to);

	TableStatus getTableCurrentStatus(UUID tableId);

	boolean deleteAllByZoneId(UUID zoneId);
}
