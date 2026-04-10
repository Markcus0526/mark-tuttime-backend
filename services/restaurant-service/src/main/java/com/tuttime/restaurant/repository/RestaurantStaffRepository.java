package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.RestaurantStaff;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantStaffRepository extends JpaRepository<RestaurantStaff, UUID> {

	List<RestaurantStaff> findByRestaurantId(UUID restaurantId);

	Optional<RestaurantStaff> findByStaffId(UUID staffId);

	boolean existsByRestaurantIdAndUserId(UUID restaurantId, UUID userId);

	boolean existsByBranchIdAndUserId(UUID branchId, UUID userId);

	boolean existsByRestaurantIdAndUserIdAndRoleName(UUID branchId, UUID userId, String roleName);

	boolean existsByBranchIdAndUserIdAndRoleName(UUID branchId, UUID userId, String roleName);
}
