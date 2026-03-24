package com.tuttime.restaurant.service;

import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZoneService {

  private final ZoneRepository zoneRepository;
  private final BranchRepository branchRepository;

  public ZoneService(ZoneRepository zoneRepository, BranchRepository branchRepository) {
    this.zoneRepository = zoneRepository;
    this.branchRepository = branchRepository;
  }

  public List<Zone> getAll() {
    return zoneRepository.findAll();
  }

  public Zone getById(Long id) {
    return zoneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + id));
  }

  public Zone create(Zone zone) {
    Long branchId = zone.getBranch().getBranchId();
    Branch branch = branchRepository.findById(branchId)
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id " + branchId));
    zone.setBranch(branch);
    return zoneRepository.save(zone);
  }

  public Zone update(Long id, Zone details) {
    Zone existing = getById(id);
    Long branchId = details.getBranch().getBranchId();
    Branch branch = branchRepository.findById(branchId)
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id " + branchId));

    existing.setBranch(branch);
    existing.setName(details.getName());
    existing.setDescription(details.getDescription());
    existing.setCapacity(details.getCapacity());

    return zoneRepository.save(existing);
  }

  public void delete(Long id) {
    Zone existing = getById(id);
    zoneRepository.delete(existing);
  }
}
