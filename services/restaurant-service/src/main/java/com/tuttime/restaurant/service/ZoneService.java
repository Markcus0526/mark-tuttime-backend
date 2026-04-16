package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.ZoneDto;

import java.util.List;
import java.util.UUID;

public interface ZoneService {

	List<ZoneDto.ZoneResponse> getZonesByBranchId(UUID branchId);

	ZoneDto.ZoneDetailedResponse getZoneById(UUID zoneId);

	ZoneDto.ZoneResponse createZone(ZoneDto.ZoneRequest request, UUID currentUserId, String userRoles);

	ZoneDto.ZoneResponse updateZone(UUID zoneId, ZoneDto.ZoneRequest request, UUID currentUserId, String userRoles);

	void deleteZone(UUID zoneId, UUID currentUserId, String userRoles);

	boolean deleteAllByBranchId(UUID branchId);
}