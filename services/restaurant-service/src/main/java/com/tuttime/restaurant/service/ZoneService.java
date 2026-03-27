package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.Zone;

public interface ZoneService {

	List<Zone> getAll();

	Zone getById(UUID id);

	Zone create(Zone zone);

	Zone update(UUID id, Zone details);

	void delete(UUID id);
}
