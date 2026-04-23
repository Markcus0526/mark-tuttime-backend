package com.tuttime.notification.service.impl;

import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.event.proto.UserDetailRequest;
import com.tuttime.common.event.proto.UserDetailResponse;
import com.tuttime.notification.entity.NotificationLog;
import com.tuttime.notification.entity.NotificationTemplate;
import com.tuttime.notification.entity.UserNotificationPreference;
import com.tuttime.notification.entity.enums.NotificationChannel;
import com.tuttime.notification.entity.enums.NotificationPriority;
import com.tuttime.notification.entity.enums.NotificationStatus;
import com.tuttime.notification.grpc.client.UserGrpcClient;
import com.tuttime.notification.provider.EmailProvider;
import com.tuttime.notification.provider.InAppProvider;
import com.tuttime.notification.repository.NotificationLogRepository;
import com.tuttime.notification.repository.NotificationTemplateRepository;
import com.tuttime.notification.repository.UserNotificationPreferenceRepository;
import com.tuttime.notification.service.MailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestratorImpl implements com.tuttime.notification.service.NotificationOrchestrator {

	private final NotificationTemplateRepository notificationTemplateRepository;
	private final UserNotificationPreferenceRepository userNotificationPreferenceRepository;
	private final NotificationLogRepository notificationLogRepository;
	private final MailTemplateService templateService;
	private final UserGrpcClient userGrpcClient;
	private final EmailProvider emailProvider;
	private final InAppProvider inAppProvider;

	@Transactional
	public void handleNotification(UUID userId, NotificationTemplateKey templateKey, Map<String, Object> params, String eventId) {
		// IDEMPOTENCY CHECK: Skip if we already sent this specific event via this channel
		if (notificationLogRepository.existsByEventId(eventId)) {
			log.info("Skipping duplicate event: {}", eventId);
			return;
		}

		// 1. Fetch User Profile via gRPC
		UserDetailResponse userResponse = userGrpcClient.getUserDetail(UserDetailRequest.newBuilder()
				.setUserId(userId.toString())
				.build());

		// 2. Load all active templates for this event (Email, SMS, etc.)
		List<NotificationTemplate> templates = notificationTemplateRepository.findByTemplateKeyAndIsActiveTrue(templateKey.name());

		for (NotificationTemplate template : templates) {
			// 3. Check User Preferences (Opt-in/Opt-out)
			if (isChannelEnabled(userId, templateKey, template.getChannel())) {

				// 4. Create Initial Audit Log (PENDING)
				NotificationLog logEntry = NotificationLog.builder()
						.userId(userId)
						.templateKey(templateKey.name())
						.channel(template.getChannel())
						.recipient(getRecipient(template.getChannel(), userResponse))
						.status(NotificationStatus.PENDING)
						.priority(NotificationPriority.MEDIUM)
						.build();

				notificationLogRepository.save(logEntry);

				// 5. Execute Dispatch based on Channel
				dispatch(template, userResponse, params, logEntry);
			}
		}
	}

	private void dispatch(NotificationTemplate template, UserDetailResponse user, Map<String, Object> params, NotificationLog logEntry) {
		try {
			switch (template.getChannel()) {
				case EMAIL -> {
					String htmlBody = templateService.generateHtmlContent(template.getContent(), params);
					emailProvider.sendHtmlEmail(user.getEmail(), template.getTitle(), htmlBody);
					logEntry.setStatus(NotificationStatus.SENT);
				}
				case SMS -> {
					// smsProvider.send(user.getPhoneNumber(), template.getContent());
					logEntry.setStatus(NotificationStatus.SENT);
				}
				case IN_APP -> {
					inAppProvider.sendRealTimeUpdate(user.getUserId(), template.getTitle(), template.getContent());
				}
				// Add Push/In-App logic here
			}
			logEntry.setSentAt(OffsetDateTime.now());
		} catch (Exception e) {
			log.error("Dispatch failed for log {}: {}", logEntry.getId(), e.getMessage());
			logEntry.setStatus(NotificationStatus.FAILED);
			logEntry.setProviderResponse(Map.of("error", e.getMessage()));
		}
		notificationLogRepository.save(logEntry);
	}

	private boolean isChannelEnabled(UUID userId, NotificationTemplateKey type, NotificationChannel channel) {
		return userNotificationPreferenceRepository.findByUserIdAndNotificationTypeAndChannel(userId, type.name(), channel)
				.map(UserNotificationPreference::isEnabled)
				.orElse(true); // Default to enabled if no preference set
	}

	private String getRecipient(NotificationChannel channel, UserDetailResponse user) {
		return (channel == NotificationChannel.EMAIL) ? user.getEmail() : user.getPhoneNumber();
	}
}

