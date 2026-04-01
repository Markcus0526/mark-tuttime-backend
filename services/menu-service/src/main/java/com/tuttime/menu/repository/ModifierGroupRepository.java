package com.tuttime.menu.repository;

import com.tuttime.menu.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {
	// Fetches groups associated with a specific branch if you decide to share groups
	// List<ModifierGroup> findByBranchId(UUID branchId);
}
