package com.tuttime.menu.repository;

import com.tuttime.menu.entity.MenuCategory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

	@Query("SELECT mc FROM MenuCategory mc WHERE mc.branchId = :branchId " +
			"AND mc.deleted = false " +
			"AND (CAST(:search AS string) IS NULL OR " + // Added CAST here
			"LOWER(mc.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
			"LOWER(mc.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
	List<MenuCategory> findActiveByBranchWithSearch(
			@Param("branchId") UUID branchId,
			@Param("search") String search,
			Sort sort);

	@Query("""
        SELECT DISTINCT c FROM MenuCategory c 
        LEFT JOIN FETCH c.items i 
        LEFT JOIN FETCH i.modifierGroups mg 
        LEFT JOIN FETCH c.modifierGroups cmg
        WHERE c.branchId = :branchId AND c.deleted = false
        ORDER BY c.priority ASC, i.priority ASC
    """)
	List<MenuCategory> findFullMenuByBranchId(@Param("branchId") UUID branchId);

	@EntityGraph(attributePaths = {
			"items",
			"items.modifierGroups",
			"modifierGroups"
	})
	List<MenuCategory> findByBranchIdAndDeletedFalseOrderByPriorityAsc(UUID branchId);

	List<MenuCategory> findAllByBranchId(UUID branchId);

	@EntityGraph(attributePaths = {"items"})
	List<MenuCategory> findAllByBranchIdOrderByPriorityAsc(UUID branchId);

	boolean existsById(UUID id);
}