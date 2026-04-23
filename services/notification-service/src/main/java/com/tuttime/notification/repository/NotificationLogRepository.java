package com.tuttime.notification.repository;

import com.tuttime.notification.entity.NotificationLog;
import com.tuttime.notification.entity.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

	// Finds failed notifications that haven't exceeded retry limits
	List<NotificationLog> findByStatusAndRetryCountLessThan(
			NotificationStatus status, Integer maxRetries);

	// Tracks history for a specific user
	List<NotificationLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

	boolean existsByEventId(String eventId);

}