package com.tuttime.menu.repository;

import com.tuttime.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

	// Fetch categories and their nested items in one SQL JOIN
	@EntityGraph(attributePaths = {"items", "items.modifierGroups", "items.modifierGroups.modifiers"})
	List<MenuCategory> findAllByBranchId(UUID branchId);

	@EntityGraph(attributePaths = {"items"})
	List<MenuCategory> findAllByBranchIdOrderByPriorityAsc(UUID branchId);

	boolean existsById(UUID id);
}