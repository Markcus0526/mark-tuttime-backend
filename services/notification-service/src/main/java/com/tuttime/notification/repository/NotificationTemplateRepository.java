package com.tuttime.notification.repository;

import com.tuttime.notification.entity.NotificationTemplate;
import com.tuttime.notification.entity.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Integer> {

	// Finds all active templates (Email, SMS, etc.) for a specific event
	List<NotificationTemplate> findByTemplateKeyAndIsActiveTrue(String templateKey);

	// Finds template by key, channel, and language for localized messaging
	Optional<NotificationTemplate> findByTemplateKeyAndChannelAndLanguageAndIsActiveTrue(
			String templateKey, NotificationChannel channel, String language);

}