package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

	Optional<Branch> findByBranchIdAndDeletedFalse(UUID branchId);

	Page<Branch> findPagesByRestaurantIdAndDeletedFalse(UUID restaurantId, Pageable pageable);

	Page<Branch> findByDeletedFalse(Pageable pageable);

	List<Branch> findAllByRestaurantIdAndDeletedFalse(UUID restaurantId);

	@Modifying
	@Query("UPDATE Branch b SET b.deleted = true, b.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE b.restaurantId = :restaurantId AND b.deleted = false")
	void deleteAllByRestaurantId(@Param("restaurantId") UUID restaurantId);

}