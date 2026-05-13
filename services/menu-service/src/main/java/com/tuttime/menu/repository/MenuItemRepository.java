package com.tuttime.menu.repository;

import com.tuttime.menu.entity.MenuItem;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

	Optional<MenuItem> findByIdAndDeletedFalse(UUID id);

	// Finds items in a category where name or description contains the search string
	@Query("SELECT mi FROM MenuItem mi WHERE mi.category.id = :categoryId " +
			"AND mi.deleted = false " +
			"AND (CAST(:search AS string) IS NULL OR " + // Added CAST here
			"LOWER(mi.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
			"LOWER(mi.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
	List<MenuItem> findByCategoryAndSearch(@Param("categoryId") UUID categoryId,
	                                       @Param("search") String search,
	                                       Sort sort);

	@Modifying
	@Transactional
	@Query("UPDATE MenuItem m SET m.available = true, m.updatedAt = CURRENT_TIMESTAMP " +
			"WHERE m.id IN :itemIds")
	void activateItemsFromPos(@Param("itemIds") List<UUID> itemIds);

	@Modifying
	@Transactional
	@Query("UPDATE MenuItem m SET m.available = false, m.updatedAt = CURRENT_TIMESTAMP " +
			"WHERE m.id IN :itemIds")
	void deactivateItemsFromPos(@Param("itemIds") List<UUID> itemIds);

	boolean existsByCategoryIdAndNameIgnoreCaseAndDeletedFalse(UUID categoryId, String name);

	boolean existsByCategoryIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(UUID categoryId, String name, UUID excludeId);

	List<MenuItem> findByCategoryIdAndDeletedFalse(UUID categoryId);
}
