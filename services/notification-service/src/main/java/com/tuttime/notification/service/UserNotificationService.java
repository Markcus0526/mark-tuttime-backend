package com.tuttime.notification.service;

import com.tuttime.notification.entity.NotificationLog;
import com.tuttime.notification.entity.enums.NotificationStatus;

import java.util.List;
import java.util.UUID;

public interface UserNotificationService {

	List<NotificationLog> getUserNotifications(UUID userId);

	void updateStatus(UUID id, NotificationStatus status);

	void clearAll(UUID userId);
}
