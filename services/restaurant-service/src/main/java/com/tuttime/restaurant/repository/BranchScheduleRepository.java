package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.BranchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BranchScheduleRepository extends JpaRepository<BranchSchedule, UUID> {

	List<BranchSchedule> findByBranchId(UUID branchId);

}