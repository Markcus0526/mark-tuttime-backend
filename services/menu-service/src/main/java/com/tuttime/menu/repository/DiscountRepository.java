package com.tuttime.menu.repository;

import com.tuttime.menu.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, UUID> {

	List<Discount> findByReferenceIdAndActiveTrue(UUID referenceId);

	@Query("""
			    SELECT d FROM Discount d 
			    WHERE d.active = true 
			    AND d.referenceId IN (:itemAndCategoryId)
			    AND (d.startDate IS NULL OR d.startDate <= CURRENT_TIMESTAMP)
			    AND (d.endDate IS NULL OR d.endDate >= CURRENT_TIMESTAMP)
			""")
	List<Discount> findActiveDiscounts(@Param("itemAndCategoryId") List<UUID> ids);
}
