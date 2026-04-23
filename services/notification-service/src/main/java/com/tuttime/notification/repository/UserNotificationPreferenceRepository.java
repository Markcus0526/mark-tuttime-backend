package com.tuttime.notification.repository;

import com.tuttime.notification.entity.UserNotificationPreference;
import com.tuttime.notification.entity.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Integer> {

	// Checks specific preference for a user
	Optional<UserNotificationPreference> findByUserIdAndNotificationTypeAndChannel(
			UUID userId, String notificationType, NotificationChannel channel);
}