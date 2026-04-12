package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.BranchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchScheduleRepository extends JpaRepository<BranchSchedule, UUID> {

	Optional<BranchSchedule> findByScheduleIdAndDeletedFalse(UUID scheduleId);

	List<BranchSchedule> findAllByBranchIdAndDeletedFalse(UUID branchId);

	@Modifying
	@Query("UPDATE BranchSchedule b SET b.deleted = true, b.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE b.branchId = :branchId AND b.deleted = false")
	void deleteAllByBranchId(@Param("branchId") UUID branchId);

}