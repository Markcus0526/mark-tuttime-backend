package com.tuttime.restaurant.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.ZoneRequest;
import com.tuttime.restaurant.dto.ZoneResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.mapper.ZoneMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.ZoneRepository;

import com.tuttime.restaurant.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ZoneServiceImpl implements ZoneService {

    private final ZoneRepository zoneRepository;
    private final BranchRepository branchRepository;
    private final ZoneMapper zoneMapper;

    @Override
    public List<ZoneResponse> getZonesByBranchId(UUID branchId) {
        List<Zone> zones = zoneRepository.findByBranchIdAndIsDeletedFalse(branchId);
        return zones.stream().map(zoneMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public ZoneResponse getZoneById(UUID branchId, UUID zoneId) {
        Zone zone = zoneRepository.findByZoneIdAndBranchIdAndIsDeletedFalse(zoneId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId + " for branch " + branchId));
        return zoneMapper.toResponse(zone);
    }

    @Override
    public ZoneResponse createZone(UUID branchId, ZoneRequest request) {
        Zone zone = zoneMapper.fromRequest(request);
        zone.setBranchId(branchId);
        zone.setDeleted(false);
        Zone savedZone = zoneRepository.save(zone);
        return zoneMapper.toResponse(savedZone);
    }

    @Override
    public ZoneResponse updateZone(UUID branchId, UUID zoneId, ZoneRequest request) {
        Zone existingZone = zoneRepository.findByZoneIdAndBranchIdAndIsDeletedFalse(zoneId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId + " for branch " + branchId));

        // Update fields from request (add all updatable fields)
        existingZone.setName(request.getName());
        existingZone.setDescription(request.getDescription());
        // Add more fields as needed

        Zone updatedZone = zoneRepository.save(existingZone);
        return zoneMapper.toResponse(updatedZone);
    }

    @Override
    public void deleteZone(UUID branchId, UUID zoneId) {
        Zone zone = zoneRepository.findByZoneIdAndBranchIdAndIsDeletedFalse(zoneId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId + " for branch " + branchId));
        zone.setDeleted(true); // Assuming soft delete
        zoneRepository.save(zone);
    }
}
