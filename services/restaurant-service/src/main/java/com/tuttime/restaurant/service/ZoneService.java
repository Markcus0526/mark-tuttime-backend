package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.ZoneRequest;
import com.tuttime.restaurant.dto.ZoneResponse;

import java.util.List;
import java.util.UUID;

public interface ZoneService {

	List<ZoneResponse> getZonesByBranchId(UUID branchId);

	ZoneResponse getZoneById(UUID branchId, UUID zoneId);

	ZoneResponse createZone(UUID branchId, ZoneRequest request);

	ZoneResponse updateZone(UUID branchId, UUID zoneId, ZoneRequest request);

	void deleteZone(UUID branchId, UUID zoneId);

	// other zone-related methods if any
}