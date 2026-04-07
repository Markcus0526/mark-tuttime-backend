package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

	Page<Branch> findByRestaurantIdAndIsDeletedFalse(UUID restaurantId, Pageable pageable);

	Page<Branch> findByIsDeletedFalse(Pageable pageable);

	Optional<Branch> findByBranchIdAndRestaurantIdAndIsDeletedFalse(UUID branchId, UUID restaurantId);

	boolean existsByBranchIdAndRestaurantIdAndManagerUserIdAndIsDeletedFalse(UUID branchId, UUID restaurantId, UUID managerUserId);
}