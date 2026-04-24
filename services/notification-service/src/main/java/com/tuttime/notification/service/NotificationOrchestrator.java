package com.tuttime.notification.service;

import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.notification.entity.enums.NotificationChannel;

import java.util.Map;
import java.util.UUID;

public interface NotificationOrchestrator {

	void handleNotification(UUID eventId, UUID userId, NotificationTemplateKey templateKey, Map<String, Object> params);


}