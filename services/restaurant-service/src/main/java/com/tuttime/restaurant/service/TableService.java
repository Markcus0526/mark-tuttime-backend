package com.tuttime.restaurant.service;

import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.TableRepository;
import com.tuttime.restaurant.repository.ZoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableService {

  private final TableRepository tableRepository;
  private final ZoneRepository zoneRepository;

  public TableService(TableRepository tableRepository, ZoneRepository zoneRepository) {
    this.tableRepository = tableRepository;
    this.zoneRepository = zoneRepository;
  }

  public List<Table> getAll() {
    return tableRepository.findAll();
  }

  public Table getById(Long id) {
    return tableRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Table not found with id " + id));
  }

  public Table create(Table table) {
    Long zoneId = table.getZone().getZoneId();
    Zone zone = zoneRepository.findById(zoneId)
            .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));
    table.setZone(zone);
    return tableRepository.save(table);
  }

  public Table update(Long id, Table details) {
    Table existing = getById(id);
    Long zoneId = details.getZone().getZoneId();
    Zone zone = zoneRepository.findById(zoneId)
            .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));

    existing.setZone(zone);
    existing.setNumber(details.getNumber());
    existing.setSeats(details.getSeats());
    existing.setStatus(details.getStatus());

    return tableRepository.save(existing);
  }

  public void delete(Long id) {
    Table existing = getById(id);
    tableRepository.delete(existing);
  }
}
