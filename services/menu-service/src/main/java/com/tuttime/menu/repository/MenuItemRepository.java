package com.tuttime.menu.repository;

import com.tuttime.menu.entity.MenuItem;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

	/**
	 * Finds all items belonging to a specific category.
	 */
	List<MenuItem> findByCategory_MenuCategoryId(UUID categoryId, Sort sort);

	/**
	 * Efficiently fetches a single menu item with all its modifier groups
	 * and modifiers in one SQL query.
	 */
	@Query("SELECT mi FROM MenuItem mi " +
			"LEFT JOIN FETCH mi.modifierGroups mg " +
			"LEFT JOIN FETCH mg.modifiers " +
			"WHERE mi.menuItemId = :menuItemId")
	Optional<MenuItem> findByIdWithModifiers(@Param("menuItemId") UUID menuItemId);

	/**
	 * Finds all available items for a specific category.
	 */
	List<MenuItem> findByCategory_MenuCategoryIdAndAvailableTrue(UUID categoryId);

	List<MenuItem> findByCategory_MenuCategoryIdOrderByPriorityAsc(UUID categoryId);
}
