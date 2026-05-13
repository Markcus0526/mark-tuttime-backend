package com.tuttime.notification.repository;

import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.notification.entity.NotificationTemplate;
import com.tuttime.notification.entity.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Integer> {

	// Finds all active templates (Email, SMS, etc.) for a specific event
	List<NotificationTemplate> findByTemplateKeyAndActiveTrue(NotificationTemplateKey templateKey);

	boolean existsByTemplateKeyAndChannelAndLanguage(NotificationTemplateKey key, NotificationChannel channel, String lang);

	Optional<NotificationTemplate> findByTemplateKeyAndChannelAndLanguageAndActiveTrue(
			NotificationTemplateKey templateKey, NotificationChannel channel, String language);
}