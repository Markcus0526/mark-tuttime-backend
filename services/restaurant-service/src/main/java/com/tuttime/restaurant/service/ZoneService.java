package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.ZoneRequest;
import com.tuttime.restaurant.dto.ZoneResponse;

import java.util.List;
import java.util.UUID;

public interface ZoneService {

	List<ZoneResponse> getZonesByBranchId(UUID branchId);

	ZoneResponse getZoneById(UUID zoneId);

	ZoneResponse createZone(ZoneRequest request, UUID currentUserId, String userRoles);

	ZoneResponse updateZone(UUID zoneId, ZoneRequest request, UUID currentUserId, String userRoles);

	void deleteZone(UUID zoneId, UUID currentUserId, String userRoles);

	// other zone-related methods if any
}