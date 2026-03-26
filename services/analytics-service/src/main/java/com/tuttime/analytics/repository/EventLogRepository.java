package com.tuttime.analytics.repository;

import com.tuttime.analytics.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, UUID> {
	// Additional query methods can be defined here, e.g. findByEventType, findByTimestampBetween, etc.
}
