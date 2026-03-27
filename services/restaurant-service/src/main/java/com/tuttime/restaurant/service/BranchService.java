package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.Branch;

public interface BranchService {

	List<Branch> getAll();

	Branch getById(UUID id);

	Branch create(Branch branch);

	Branch update(UUID id, Branch details);

	void delete(UUID id);
}
