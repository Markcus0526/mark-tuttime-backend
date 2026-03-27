package com.tuttime.restaurant.service.impl;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.TableRepository;
import com.tuttime.restaurant.repository.ZoneRepository;

import com.tuttime.restaurant.service.TableService;
import org.springframework.stereotype.Service;

@Service
public class TableServiceImpl implements TableService {

    private final TableRepository tableRepository;
    private final ZoneRepository zoneRepository;

    public TableServiceImpl(TableRepository tableRepository, ZoneRepository zoneRepository) {
        this.tableRepository = tableRepository;
        this.zoneRepository = zoneRepository;
    }

    @Override
    public List<Table> getAll() {
        return tableRepository.findAll();
    }

    @Override
    public Table getById(UUID id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id " + id));
    }

    @Override
    public Table create(Table table) {
        UUID zoneId = table.getZone().getZoneId();
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));
        table.setZone(zone);
        return tableRepository.save(table);
    }

    @Override
    public Table update(UUID id, Table details) {
        Table existing = getById(id);
        UUID zoneId = details.getZone().getZoneId();
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id " + zoneId));

        existing.setZone(zone);
        existing.setNumber(details.getNumber());
        existing.setSeats(details.getSeats());
        existing.setStatus(details.getStatus());

        return tableRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        Table existing = getById(id);
        tableRepository.delete(existing);
    }
}
