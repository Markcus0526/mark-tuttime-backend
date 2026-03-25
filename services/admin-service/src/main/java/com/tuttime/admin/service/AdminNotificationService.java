package com.tuttime.admin.service;

import com.tuttime.admin.dto.NotificationRequest;
import com.tuttime.admin.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

/**
 * Admin Notification Service Interface
 */
public interface AdminNotificationService {
	Page<NotificationResponse> listNotifications(Pageable pageable, String status);

	NotificationResponse getNotification(Long id);

	NotificationResponse createNotification(NotificationRequest request);

	NotificationResponse updateNotification(Long id, NotificationRequest request);

	void deleteNotification(Long id);

	NotificationResponse publishNotification(Long id);

	NotificationResponse cancelNotification(Long id);

	NotificationResponse scheduleNotification(Long id, OffsetDateTime scheduledAt);
}

