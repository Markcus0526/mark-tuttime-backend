package com.tuttime.notification.service.impl;

import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.event.proto.DeviceTokenDeleteRequest;
import com.tuttime.common.event.proto.UserDetailRequest;
import com.tuttime.common.event.proto.UserDetailResponse;
import com.tuttime.notification.entity.NotificationLog;
import com.tuttime.notification.entity.NotificationTemplate;
import com.tuttime.notification.entity.UserNotificationPreference;
import com.tuttime.notification.entity.enums.NotificationChannel;
import com.tuttime.notification.entity.enums.NotificationPriority;
import com.tuttime.notification.entity.enums.NotificationStatus;
import com.tuttime.notification.grpc.client.UserGrpcClient;
import com.tuttime.notification.provider.*;
import com.tuttime.notification.repository.NotificationLogRepository;
import com.tuttime.notification.repository.NotificationTemplateRepository;
import com.tuttime.notification.repository.UserNotificationPreferenceRepository;
import com.tuttime.notification.service.TemplateService;
import com.tuttime.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestratorImpl implements NotificationOrchestrator {

	private final NotificationTemplateRepository notificationTemplateRepository;
	private final UserNotificationPreferenceRepository userNotificationPreferenceRepository;
	private final NotificationLogRepository notificationLogRepository;
	private final TemplateService templateService;
	private final UserGrpcClient userGrpcClient;
	private final EmailProvider emailProvider;
	private final SmsProvider smsProvider;
	private final InAppProvider inAppProvider;
	private final PushProvider pushProvider;
	private final UserSessionRegistry userSessionRegistry;

	@Transactional
	public void handleNotification(UUID eventId, UUID userId, NotificationTemplateKey templateKey, Map<String, Object> params) {

		// 1. Fetch User Profile via gRPC
		// Note: We do this once outside the loop to save network calls.
		UserDetailResponse userResponse = userGrpcClient.getUserDetail(UserDetailRequest.newBuilder()
				.setUserId(userId.toString())
				.build());

		// 2. Load all active templates for this event (Email, SMS, IN_APP)
		List<NotificationTemplate> templates = notificationTemplateRepository.findByTemplateKeyAndActiveTrue(templateKey);

		for (NotificationTemplate template : templates) {

			// NEW: Check if the user actually has the data required for this channel
			if (!isChannelSupported(template.getChannel(), userResponse)) {
				log.warn("Channel {} is active but user {} is missing required contact info. Skipping.",
						template.getChannel(), userId);
				continue;
			}

			// 1. Find if a log already exists for this event and channel
			Optional<NotificationLog> existingLog = notificationLogRepository
					.findByEventIdAndChannel(eventId, template.getChannel());

			// 2. If it exists and was already successful, skip entirely
			if (existingLog.isPresent() &&
					(existingLog.get().getStatus() == NotificationStatus.SENT ||
							existingLog.get().getStatus() == NotificationStatus.DELIVERED)) {
				log.info("Already sent successfully for channel {}. Skipping.", template.getChannel());
				continue;
			}

			// 3. Prepare the logEntry (either use the existing one to retry or build a new one)
			NotificationLog logEntry;
			if (existingLog.isPresent()) {
				logEntry = existingLog.get();
				logEntry.setStatus(NotificationStatus.PENDING); // Reset to pending for retry
				logEntry.setRetryCount(logEntry.getRetryCount() + 1);
			} else {
				logEntry = NotificationLog.builder()
						.userId(userId)
						.eventId(eventId)
						.templateKey(templateKey.name())
						.channel(template.getChannel())
						.recipient(getRecipient(template.getChannel(), userResponse))
						.status(NotificationStatus.PENDING)
						.priority(NotificationPriority.MEDIUM)
						.retryCount(0)
						.build();
			}

			// 4. saveAndFlush will now either INSERT or UPDATE (no more Duplicate Key error)
			logEntry = notificationLogRepository.saveAndFlush(logEntry);

			// 5. Execute Dispatch
			dispatch(template, userResponse, params, logEntry);
		}
	}

	private void dispatch(NotificationTemplate template, UserDetailResponse user, Map<String, Object> params, NotificationLog logEntry) {
		try {
			String processedContent = templateService.generateHtmlContent(template.getContent(), params);

			switch (template.getChannel()) {
				case EMAIL -> {
					String htmlBody = templateService.generateHtmlContent(template.getContent(), params);
					emailProvider.sendHtmlEmail(user.getEmail(), template.getTitle(), htmlBody, logEntry.getId());
					logEntry.setStatus(NotificationStatus.SENT);
				}
				case SMS -> {
					if (!smsProvider.isLocallyValid(user.getPhoneNumber())) {
						logEntry.setStatus(NotificationStatus.FAILED);
						logEntry.setProviderResponse(Map.of("error", "Invalid phone number format"));
					} else {
						String smsBody = templateService.generateHtmlContent(template.getContent(), params);
						smsProvider.send(user.getPhoneNumber(), smsBody);
						logEntry.setStatus(NotificationStatus.SENT);
					}
				}
				case IN_APP -> {
					// 1. WebSocket: Always try to send to active browser/app sessions first
					if (userSessionRegistry.isUserOnline(user.getUserId())) {
						inAppProvider.sendRealTimeUpdate(user.getUserId(), template.getTitle(), processedContent);
						logEntry.setProviderResponse(Map.of("method", "WEBSOCKET"));
						logEntry.setStatus(NotificationStatus.SENT);
					}

					// 2. Fallback/Parallel Push: Send to all registered Firebase devices
					List<String> tokens = user.getFcmTokensList(); // Updated to handle 'repeated' proto field

					if (!tokens.isEmpty()) {
						for (String token : tokens) {
							Thread.ofVirtual().start(() -> {
								try {
									pushProvider.sendPush(token, template.getTitle(), processedContent);
								} catch (Exception e) {
									if (isTokenInvalid(e)) {
										log.info("Reporting invalid token to User Service: {}", token);
										// Call gRPC client to tell User Service to delete this token
										userGrpcClient.deleteDeviceToken(DeviceTokenDeleteRequest.newBuilder()
												.setFcmToken(token)
												.build());
									}
								}
							});
						}

						// If WebSocket wasn't used, mark Push as the primary method
						if (logEntry.getProviderResponse() == null) {
							logEntry.setProviderResponse(Map.of("method", "FIREBASE_PUSH", "device_count", tokens.size()));
						}
						logEntry.setStatus(NotificationStatus.SENT);
					} else if (!userSessionRegistry.isUserOnline(user.getUserId())) {
						// Only mark as warning if BOTH WebSocket and Push are unavailable
						logEntry.setProviderResponse(Map.of("warning", "User offline and no FCM tokens registered"));
						logEntry.setStatus(NotificationStatus.FAILED);
					}
				}
			}
			logEntry.setSentAt(OffsetDateTime.now());
		} catch (Exception e) {
			log.error("Dispatch failed for log {}: {}", logEntry.getId(), e.getMessage());
			logEntry.setStatus(NotificationStatus.FAILED);
			logEntry.setProviderResponse(Map.of("error", e.getMessage()));
		}
		notificationLogRepository.save(logEntry);
	}

	private boolean isChannelEnabled(UUID userId, NotificationTemplateKey templateKey, NotificationChannel channel) {
		// 1. Get the broad category (e.g., "BOOKING")
		String category = getPreferenceCategory(templateKey);

		// 2. Query the DB using the category
		return userNotificationPreferenceRepository
				.findByUserIdAndNotificationTypeAndChannel(userId, category, channel)
				.map(UserNotificationPreference::isEnabled)
				.orElse(true); // Default to enabled for transactional/important alerts
	}

	private String getPreferenceCategory(NotificationTemplateKey key) {
		return switch (key) {
			case USER_REGISTER, USER_VERIFICATION, USER_LOGIN, PASSWORD_RESET, MFA_CODE, ACCOUNT_LOCKED, ADMIN_USER_CREATED -> "ACCOUNT";
			case BOOKING_CREATED, BOOKING_REMINDER, BOOKING_CANCELED, BOOKING_COMPLETED, MANAGER_BOOKING_ALERT, MANAGER_BOOKING_CANCEL, MANAGER_LOW_AVAILABILITY -> "BOOKING";
			case PAYMENT_SUCCESS, PAYMENT_FAILED, PAYMENT_REFUNDED -> "PAYMENT";
			case REVIEW_REQUEST, REVIEW_APPROVED, REVIEW_REJECTED -> "REVIEW";
		};
	}

	/**
	 * Selects the correct contact detail from the gRPC user profile based on channel.
	 */
	private String getRecipient(NotificationChannel channel, UserDetailResponse user) {
		return switch (channel) {
			case EMAIL -> user.getEmail();
			case SMS -> user.getPhoneNumber();
			case IN_APP -> user.getUserId(); // For WebSocket routing
			default -> throw new IllegalArgumentException("Unsupported channel: " + channel);
		};
	}

	private boolean isTokenInvalid(Exception e) {
		String msg = e.getMessage().toLowerCase();
		// 'registration-token-not-registered' is the standard FCM error for uninstalled apps
		return msg.contains("not-registered") ||
				msg.contains("invalid-registration-token") ||
				msg.contains("not-found");
	}

	private boolean isChannelSupported(NotificationChannel channel, UserDetailResponse user) {
		return switch (channel) {
			case EMAIL -> !user.getEmail().isBlank();
			case SMS -> !user.getPhoneNumber().isBlank();
			case IN_APP -> userSessionRegistry.isUserOnline(user.getUserId()) || !user.getFcmTokensList().isEmpty();
			default -> false;
		};
	}
}

