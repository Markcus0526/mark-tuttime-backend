package com.tuttime.notification.scheduler;

import com.tuttime.notification.entity.enums.NotificationStatus;
import com.tuttime.notification.provider.EmailProvider;
import com.tuttime.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {
	private final NotificationLogRepository notificationLogRepository;
	private final EmailProvider emailProvider;

	@Scheduled(fixedDelay = 300000) // Every 5 minutes
	public void retryFailedNotifications() {
		var failedLogs = notificationLogRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);

		for (var log : failedLogs) {
			log.setRetryCount(log.getRetryCount() + 1);
			// Re-trigger sending logic...
		}
	}
}

