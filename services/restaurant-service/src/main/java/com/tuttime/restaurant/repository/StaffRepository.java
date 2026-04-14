package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {

	List<Staff> findAllByRestaurantId(UUID restaurantId);

	Optional<Staff> findByUserId(UUID userId);

	boolean existsByRestaurantIdAndUserId(UUID restaurantId, UUID userId);

	boolean existsByBranchIdAndUserId(UUID branchId, UUID userId);

	boolean existsByRestaurantIdAndUserIdAndRoleName(UUID branchId, UUID userId, String roleName);

	boolean existsByBranchIdAndUserIdAndRoleName(UUID branchId, UUID userId, String roleName);
}
