package com.tuttime.menu.repository;

import com.tuttime.menu.entity.Discount;
import com.tuttime.menu.entity.enums.AppliesTo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, UUID> {

	Optional<Discount> findByIdAndDeletedFalse(UUID id);

	@Query("""
			    SELECT d FROM Discount d 
			    WHERE d.active = true 
			    AND d.referenceId IN (:itemAndCategoryId)
			    AND (d.startDate IS NULL OR d.startDate <= CURRENT_TIMESTAMP)
			    AND (d.endDate IS NULL OR d.endDate >= CURRENT_TIMESTAMP)
			""")
	List<Discount> findActiveDiscounts(@Param("itemAndCategoryId") List<UUID> ids);

	@Query("""
			    SELECT d FROM Discount d 
			    WHERE d.active = true 
			    AND (d.startDate IS NULL OR d.startDate <= CURRENT_TIMESTAMP)
			    AND (d.endDate IS NULL OR d.endDate >= CURRENT_TIMESTAMP)
			    AND (
			        (d.appliesTo = 'ITEM' AND d.referenceId IN :itemIds) OR 
			        (d.appliesTo = 'CATEGORY' AND d.referenceId IN :categoryIds)
			    )
			""")
	List<Discount> findActiveDiscounts(@Param("itemIds") List<UUID> itemIds,
	                                   @Param("categoryIds") List<UUID> categoryIds);

	// For duplicate name check (create)
	boolean existsByBranchIdAndNameIgnoreCaseAndDeletedFalse(UUID branchId, String name);

	// For duplicate name check (update, excluding current id)
	boolean existsByBranchIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(UUID branchId, String name, UUID excludeId);

	// For fetching active discounts (with date filtering in service)
	List<Discount> findByBranchIdAndAppliesToAndReferenceIdAndActiveTrueAndDeletedFalse(
			UUID branchId, AppliesTo appliesTo, UUID referenceId);

	// For ORDER type (no reference)
	List<Discount> findByBranchIdAndAppliesToAndActiveTrueAndDeletedFalse(
			UUID branchId, AppliesTo appliesTo);
}
