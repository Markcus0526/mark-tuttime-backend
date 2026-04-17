package com.tuttime.menu.repository;

import com.tuttime.menu.entity.MenuItem;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

	/**
	 * Finds all items belonging to a specific category.
	 */
	List<MenuItem> findByCategoryId(UUID categoryId, Sort sort);

	// Finds items in a category where name or description contains the search string
	@Query("SELECT mi FROM MenuItem mi WHERE mi.category.id = :categoryId " +
			"AND mi.deleted = false " +
			"AND (CAST(:search AS string) IS NULL OR " + // Added CAST here
			"LOWER(mi.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
			"LOWER(mi.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
	List<MenuItem> findByCategoryAndSearch(@Param("categoryId") UUID categoryId,
	                                       @Param("search") String search,
	                                       Sort sort);

	/**
	 * Efficiently fetches a single menu item with all its modifier groups
	 * and modifiers in one SQL query.
	 */
	@Query("SELECT mi FROM MenuItem mi " +
			"LEFT JOIN FETCH mi.modifierGroups mg " +
			"LEFT JOIN FETCH mg.modifiers " +
			"WHERE mi.id = :id")
	Optional<MenuItem> findByIdWithModifiers(@Param("id") UUID menuItemId);

	/**
	 * Finds all available items for a specific category.
	 */
	List<MenuItem> findByCategoryIdAndAvailableTrue(UUID categoryId);

	List<MenuItem> findByCategoryIdOrderByPriorityAsc(UUID categoryId);

	@EntityGraph(attributePaths = {"modifierGroups", "modifierGroups.options"})
	List<MenuItem> findAllByIdIn(Collection<UUID> ids);
}
