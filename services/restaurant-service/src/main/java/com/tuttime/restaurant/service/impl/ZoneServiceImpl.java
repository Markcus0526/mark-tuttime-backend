package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.ZoneRequest;
import com.tuttime.restaurant.dto.ZoneResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.mapper.ZoneMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.RestaurantStaffService;
import com.tuttime.restaurant.service.TableService;
import com.tuttime.restaurant.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ZoneServiceImpl implements ZoneService {

	private final RestaurantStaffService restaurantStaffService;
	private final ZoneRepository zoneRepository;
	private final BranchRepository branchRepository;
	private final ZoneMapper zoneMapper;
	private final TableService tableService;
	private final MediaService mediaService;

	@Override
	public List<ZoneResponse> getZonesByBranchId(UUID branchId) {
		List<Zone> zones = zoneRepository.findAllByBranchIdAndDeletedFalse(branchId);
		return zones.stream().map(zoneMapper::toResponse).collect(Collectors.toList());
	}

	@Override
	public ZoneResponse getZoneById(UUID zoneId) {
		Zone zone = zoneRepository.findByZoneIdAndDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));
		return zoneMapper.toResponse(zone);
	}

	@Override
	public ZoneResponse createZone(ZoneRequest request, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByBranchIdAndDeletedFalse(request.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));

		restaurantStaffService.validatePermissions(branch.getBranchId(), currentUserId, userRoles);

		Zone zone = zoneMapper.toEntity(request);

		zone.setBranchId(request.getBranchId());
		zone.setDeleted(false);
		Zone savedZone = zoneRepository.save(zone);
		return zoneMapper.toResponse(savedZone);
	}

	@Override
	public ZoneResponse updateZone(UUID zoneId, ZoneRequest request, UUID currentUserId, String userRoles) {
		Zone zone = zoneRepository.findByZoneIdAndDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));

		restaurantStaffService.validatePermissions(zone.getBranchId(), currentUserId, userRoles);

		zoneMapper.updateEntityFromRequest(request, zone);

		// No need for .save(zone) - Hibernate dirty checking handles it
		return zoneMapper.toResponse(zone);
	}

	@Override
	public void deleteZone(UUID zoneId, UUID currentUserId, String userRoles) {
		Zone zone = zoneRepository.findByZoneIdAndDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));

		restaurantStaffService.validatePermissions(zone.getBranchId(), currentUserId, userRoles);

		zone.setDeleted(true); // Assuming soft delete
		zoneRepository.save(zone);
	}

	@Override
	public boolean deleteAllByBranchId(UUID branchId) {
		List<Zone> zones = zoneRepository.findAllByBranchIdAndDeletedFalse(branchId);
		if (zones.isEmpty()) {
			return true;
		}

		for (Zone zone : zones) {
			if (!tableService.deleteAllByZoneId(zone.getZoneId())) {
				return false;
			}

			mediaService.deleteAllByEntityId(zone.getZoneId());
		}
		zoneRepository.deleteAllByBranchId(branchId);
		return true;
	}
}
