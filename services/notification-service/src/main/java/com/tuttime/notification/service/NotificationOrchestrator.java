package com.tuttime.notification.service;

import com.tuttime.common.enums.NotificationTemplateKey;

import java.util.Map;
import java.util.UUID;

public interface NotificationOrchestrator {

	void handleNotification(UUID userId, NotificationTemplateKey templateKey, Map<String, Object> params, String eventId);


}