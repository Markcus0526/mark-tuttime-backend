package com.tuttime.analytics.repository;

import com.tuttime.analytics.entity.SystemEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemEventRepository extends JpaRepository<SystemEvent, UUID> {

    // Find all events for a specific branch (useful for report generation)
    List<SystemEvent> findAllByBranchIdOrderByOccurredAtDesc(UUID branchId);
}