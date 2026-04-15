package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.mapper.ZoneMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.StaffService;
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
public class ZoneServiceImpl implements ZoneService {

	private final StaffService staffService;
	private final ZoneRepository zoneRepository;
	private final BranchRepository branchRepository;
	private final ZoneMapper zoneMapper;
	private final TableService tableService;
	private final MediaService mediaService;

	@Override
	@Transactional(readOnly = true)
	public List<ZoneDto.ZoneResponse> getZonesByBranchId(UUID branchId) {
		List<Zone> zones = zoneRepository.findAllByBranchIdAndDeletedFalse(branchId);
		return zones.stream().map(zoneMapper::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public ZoneDto.ZoneResponse getZoneById(UUID zoneId) {
		Zone zone = zoneRepository.findByIdAndDeletedFalse(zoneId)
				.orElseThrow(() -> new InvalidRequestException("Zone not found with id " + zoneId));
		return zoneMapper.toResponse(zone);
	}

	@Override
	@Transactional
	public ZoneDto.ZoneResponse createZone(ZoneDto.ZoneRequest request, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByIdAndDeletedFalse(request.branchId())
				.orElseThrow(() -> new InvalidRequestException("Branch not found: " + request.branchId()));

		staffService.validatePermissions(branch.getId(), currentUserId, userRoles);

		Zone zone = zoneMapper.toEntity(request);

		zone.setBranch(branch);
		zone.setDeleted(false);
		Zone savedZone = zoneRepository.save(zone);
		return zoneMapper.toResponse(savedZone);
	}

	@Override
	@Transactional
	public ZoneDto.ZoneResponse updateZone(UUID zoneId, ZoneDto.ZoneRequest request, UUID currentUserId, String userRoles) {
		Zone zone = zoneRepository.findByIdAndDeletedFalse(zoneId)
				.orElseThrow(() -> new InvalidRequestException("Zone not found with id " + zoneId));

		staffService.validatePermissions(zone.getBranch().getId(), currentUserId, userRoles);

		zoneMapper.updateEntityFromRequest(request, zone);

		// No need for .save(zone) - Hibernate dirty checking handles it
		return zoneMapper.toResponse(zone);
	}

	@Override
	@Transactional
	public void deleteZone(UUID zoneId, UUID currentUserId, String userRoles) {
		Zone zone = zoneRepository.findByIdAndDeletedFalse(zoneId)
				.orElseThrow(() -> new InvalidRequestException("Zone not found with id " + zoneId));

		staffService.validatePermissions(zone.getBranch().getId(), currentUserId, userRoles);

		zone.setDeleted(true); // Assuming soft delete
		zoneRepository.save(zone);
	}

	@Override
	@Transactional
	public boolean deleteAllByBranchId(UUID branchId) {
		List<Zone> zones = zoneRepository.findAllByBranchIdAndDeletedFalse(branchId);
		if (zones.isEmpty()) {
			return true;
		}

		for (Zone zone : zones) {
			if (!tableService.deleteAllByZoneId(zone.getId())) {
				return false;
			}

			mediaService.deleteAllByEntityId(zone.getId());
		}
		zoneRepository.deleteAllByBranchId(branchId);
		return true;
	}
}
