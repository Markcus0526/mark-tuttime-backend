package com.tuttime.notification.repository;

import com.tuttime.notification.entity.NotificationLog;
import com.tuttime.notification.entity.enums.NotificationChannel;
import com.tuttime.notification.entity.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

	// Finds failed notifications that haven't exceeded retry limits
	List<NotificationLog> findByStatusAndRetryCountLessThan(
			NotificationStatus status, Integer maxRetries);

	// Check if a SUCCESSFUL log exists for this event and channel
	Optional<NotificationLog> findByEventIdAndChannel(UUID eventId, NotificationChannel channel);

	// Fetches only the 50 most recent in-app alerts for the bell icon
	List<NotificationLog> findTop50ByUserIdAndChannelOrderByCreatedAtDesc(
			UUID userId,
			NotificationChannel channel
	);

	// Used for the "Clear All" functionality
	void deleteByUserIdAndChannel(UUID userId, NotificationChannel channel);

	boolean existsByEventIdAndChannel(UUID eventId, NotificationChannel channel);

	@Query("SELECT l FROM NotificationLog l WHERE l.status IN :statuses AND l.createdAt < :cutoff AND l.retryCount < :maxRetries")
	List<NotificationLog> findRetryableLogs(
			@Param("statuses") Set<NotificationStatus> statuses,
			@Param("cutoff") OffsetDateTime cutoff,
			@Param("maxRetries") int maxRetries
	);
}