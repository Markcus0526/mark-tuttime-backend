package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.ZoneRequest;
import com.tuttime.restaurant.dto.ZoneResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.mapper.ZoneMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import com.tuttime.restaurant.service.RestaurantStaffService;
import com.tuttime.restaurant.service.ZoneService;
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
@Transactional
public class ZoneServiceImpl implements ZoneService {

	private final RestaurantStaffService restaurantStaffService;
	private final ZoneRepository zoneRepository;
	private final BranchRepository branchRepository;
	private final ZoneMapper zoneMapper;

	@Override
	public List<ZoneResponse> getZonesByBranchId(UUID branchId) {
		List<Zone> zones = zoneRepository.findByBranchIdAndIsDeletedFalse(branchId);
		return zones.stream().map(zoneMapper::toResponse).collect(Collectors.toList());
	}

	@Override
	public ZoneResponse getZoneById(UUID zoneId) {
		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));
		return zoneMapper.toResponse(zone);
	}

	@Override
	public ZoneResponse createZone(ZoneRequest request, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByBranchIdAndIsDeletedFalse(request.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));

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

		Zone zone = zoneMapper.toEntity(request);

		zone.setBranchId(request.getBranchId());
		zone.setDeleted(false);
		Zone savedZone = zoneRepository.save(zone);
		return zoneMapper.toResponse(savedZone);
	}

	@Override
	public ZoneResponse updateZone(UUID zoneId, ZoneRequest request, UUID currentUserId, String userRoles) {
		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));

		Branch branch = branchRepository.findByBranchIdAndIsDeletedFalse(zone.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + zone.getBranchId()));

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

		// Update fields from request (add all updatable fields)
		zone.setName(request.getName());
		zone.setDescription(request.getDescription());
		// Add more fields as needed

		Zone updatedZone = zoneRepository.save(zone);
		return zoneMapper.toResponse(updatedZone);
	}

	@Override
	public void deleteZone(UUID zoneId, UUID currentUserId, String userRoles) {
		Zone zone = zoneRepository.findByZoneIdAndIsDeletedFalse(zoneId)
				.orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));

		Branch branch = branchRepository.findByBranchIdAndIsDeletedFalse(zone.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + zone.getBranchId()));

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

		zone.setDeleted(true); // Assuming soft delete
		zone.setDeletedAt(OffsetDateTime.now());
		zoneRepository.save(zone);
	}
}
