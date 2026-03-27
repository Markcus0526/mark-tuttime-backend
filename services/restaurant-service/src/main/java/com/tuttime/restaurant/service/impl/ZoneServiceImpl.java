package com.tuttime.restaurant.service.impl;

import java.util.List;
import java.util.UUID;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
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

    @Override
    public List<Zone> getAll() {
        return zoneRepository.findAll();
    }

    @Override
    public Zone getById(UUID id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + id));
    }

    @Override
    public Zone create(Zone zone) {
        UUID branchId = zone.getBranch().getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id " + branchId));
        zone.setBranch(branch);
        return zoneRepository.save(zone);
    }

    @Override
    public Zone update(UUID id, Zone details) {
        Zone existing = getById(id);
        UUID branchId = details.getBranch().getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id " + branchId));

        existing.setBranch(branch);
        existing.setName(details.getName());
        existing.setDescription(details.getDescription());
        existing.setCapacity(details.getCapacity());

        return zoneRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        Zone existing = getById(id);
        zoneRepository.delete(existing);
    }
}
