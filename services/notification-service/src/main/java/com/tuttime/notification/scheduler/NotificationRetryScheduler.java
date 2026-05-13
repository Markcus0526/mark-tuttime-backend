package com.tuttime.notification.scheduler;

import com.tuttime.notification.entity.NotificationLog;
import com.tuttime.notification.entity.enums.NotificationStatus;
import com.tuttime.notification.provider.EmailProvider;
import com.tuttime.notification.repository.NotificationLogRepository;
import com.tuttime.notification.service.NotificationOrchestrator;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {
	private final NotificationLogRepository notificationLogRepository;
	private final NotificationOrchestrator orchestrator;

	@Scheduled(fixedDelayString = "${notification.retry.interval.ms:300000}")
	public void retryFailedOrStuckNotifications() {
		List<NotificationLog> notificationLogs = notificationLogRepository.findRetryableLogs(
				Set.of(NotificationStatus.FAILED, NotificationStatus.PENDING),
				OffsetDateTime.now().minusMinutes(5),
				3
		);
		for (NotificationLog notificationLog : notificationLogs) {
			try {
				notificationLog.setRetryCount(notificationLog.getRetryCount() + 1);
				notificationLog = notificationLogRepository.saveAndFlush(notificationLog); // version check if using @Version

				// Re-dispatch using same logic as original
				// You need to re-fetch template and user data
				// For simplicity, you can call a new method: orchestrator.retryNotification(log.getId())
				orchestrator.retryNotification(notificationLog.getId());
			} catch (OptimisticLockException e) {
				log.warn("Log {} already processed by another node", notificationLog.getId());
			} catch (Exception e) {
				log.error("Retry failed for log {}", notificationLog.getId(), e);
				// status remains FAILED, will be retried again
			}
		}
	}
}

