package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.BranchSchedule;
import com.tuttime.restaurant.entity.RestaurantSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchScheduleRepository extends JpaRepository<BranchSchedule, UUID> {

	Optional<BranchSchedule> findByScheduleIdAndIsDeletedFalse(UUID scheduleId);

	List<BranchSchedule> findByBranchIdAndIsDeletedFalse(UUID branchId);

}