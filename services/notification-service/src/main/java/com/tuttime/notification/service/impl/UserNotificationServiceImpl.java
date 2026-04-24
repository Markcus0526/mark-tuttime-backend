package com.tuttime.notification.service.impl;


import com.tuttime.notification.entity.NotificationLog;
import com.tuttime.notification.entity.enums.NotificationChannel;
import com.tuttime.notification.entity.enums.NotificationStatus;
import com.tuttime.notification.repository.NotificationLogRepository;
import com.tuttime.notification.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

	private final NotificationLogRepository logRepository;

	public List<NotificationLog> getUserNotifications(UUID userId) {
		// Only return IN_APP logs, sorted by newest first
		return logRepository.findTop50ByUserIdAndChannelOrderByCreatedAtDesc(
				userId, NotificationChannel.IN_APP);
	}

	@Transactional
	public void updateStatus(UUID id, NotificationStatus status) {
		logRepository.findById(id).ifPresent(log -> {
			log.setStatus(status);
			logRepository.save(log);
		});
	}

	@Transactional
	public void clearAll(UUID userId) {
		// Typically we "cancel" or "delete" them so they don't show in the UI
		logRepository.deleteByUserIdAndChannel(userId, NotificationChannel.IN_APP);
	}
}
