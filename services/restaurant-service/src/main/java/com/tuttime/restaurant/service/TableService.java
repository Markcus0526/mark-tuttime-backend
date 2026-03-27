package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.Table;

public interface TableService {

	List<Table> getAll();

	Table getById(UUID id);

	Table create(Table table);

	Table update(UUID id, Table details);

	void delete(UUID id);
}
