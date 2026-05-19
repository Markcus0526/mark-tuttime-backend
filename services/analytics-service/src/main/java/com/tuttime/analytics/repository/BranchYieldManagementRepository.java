package com.tuttime.analytics.repository;

import com.tuttime.analytics.entity.BranchYieldManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchYieldManagementRepository extends JpaRepository<BranchYieldManagement, BranchYieldManagement.BranchSlotId> {

    // Find occupancy for a specific 15-minute slot
    @Query("SELECT b FROM BranchYieldManagement b WHERE b.id.branchId = :branchId AND b.id.timeSlot = :timeSlot")
    Optional<BranchYieldManagement> findByBranchAndTime(@Param("branchId") UUID branchId,
                                                        @Param("timeSlot") OffsetDateTime timeSlot);

    @Query("SELECT AVG(b.occupancyRate) FROM BranchYieldManagement b")
    Double calculateAveragePlatformOccupancy();

    List<BranchYieldManagement> findAllByIdBranchIdOrderByIdTimeSlotAsc(UUID branchId);
}