package com.tuttime.notification.service.impl;

import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.util.UuidUtil;
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
import com.tuttime.notification.service.NotificationOrchestrator;
import com.tuttime.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

	private final NotificationTemplateRepository templateRepository;
	private final UserNotificationPreferenceRepository preferenceRepository;
	private final NotificationLogRepository logRepository;
	private final TemplateService templateService;
	private final UserGrpcClient userGrpcClient;
	private final EmailProvider emailProvider;
	private final SmsProvider smsProvider;
	private final InAppProvider inAppProvider;
	private final PushProvider pushProvider;
	private final UserSessionRegistry userSessionRegistry;

	// ================================
	// Public entry point for new notifications
	// ================================

	@Override
	@Transactional
	public void handleNotification(UUID eventId,
	                               UUID userId,
	                               NotificationTemplateKey templateKey,
	                               Map<String, Object> params) {
		// 1. Fetch user profile (includes language, contacts, FCM tokens)
		GetUserDetailResponse user = fetchUserProfile(userId);
		String userLanguage = getUserLanguage(user);

		// 2. For each possible channel (EMAIL, SMS, IN_APP)
		for (NotificationChannel channel : NotificationChannel.values()) {
			if (!isChannelSupported(channel, user)) {
				log.debug("Channel {} not supported for user {}", channel, userId);
				continue;
			}
			if (!isChannelEnabled(userId, templateKey, channel)) {
				log.debug("User {} disabled channel {} for category {}", userId, channel, templateKey);
				continue;
			}

			// 3. Load template for this channel and user language (fallback to "en")
			NotificationTemplate template;
			try {
				template = findTemplateForUser(templateKey, channel, userLanguage);
			} catch (Exception e) {
				log.warn("No active template found for {} / {} / {}. Skipping channel.",
						templateKey, channel, userLanguage);
				continue;
			}

			// 4. Idempotency: use DB unique constraint on (event_id, channel)
			NotificationLog logEntry = prepareLogEntry(eventId, userId, channel, template, user);
			try {
				logEntry = logRepository.save(logEntry);
			} catch (DataIntegrityViolationException ex) {
				// Duplicate (eventId, channel) – already exists
				logEntry = logRepository.findByEventIdAndChannel(eventId, channel)
						.orElseThrow(() -> new IllegalStateException("Unique constraint violated but log not found"));
				if (logEntry.getStatus() == NotificationStatus.SENT ||
						logEntry.getStatus() == NotificationStatus.DELIVERED) {
					log.info("Event {} already sent via {}, skipping", eventId, channel);
					continue;
				}
				// Otherwise it's a stuck PENDING or FAILED log – retry
				logEntry.setRetryCount(logEntry.getRetryCount() + 1);
				logEntry = logRepository.save(logEntry);
			}

			// 5. Dispatch the notification (will update status and sent_at)
			dispatch(logEntry, template, user, params);
		}
	}

	// ================================
	// Public retry method for scheduler
	// ================================

	@Override
	@Transactional
	public void retryNotification(UUID logId) {
		NotificationLog notificationLog = logRepository.findById(logId)
				.orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

		// Only retry if failed or pending for too long (already filtered by scheduler)
		if (notificationLog.getStatus() == NotificationStatus.SENT || notificationLog.getStatus() == NotificationStatus.DELIVERED) {
			log.warn("Retry called for already delivered log {}", logId);
			return;
		}

		// Increment retry count (will be saved in dispatch)
		notificationLog.setRetryCount(notificationLog.getRetryCount() + 1);
		notificationLog.setStatus(NotificationStatus.PENDING); // reset to pending before retry

		// Re-fetch dependent data
		GetUserDetailResponse user = fetchUserProfile(notificationLog.getUserId());
		NotificationTemplate template = templateRepository.findById(notificationLog.getTemplateId())
				.orElseThrow(() -> new InvalidRequestException("Invalid: template key: " + notificationLog.getTemplateKey() + ", channel: " + notificationLog.getChannel()));

		// Reconstruct params? For simplicity, we assume params are not needed for retry
		// because the original event data should be stored elsewhere, but we can pass an empty map.
		// Better: store serialized params in the log table (JSONB). For brevity, we pass empty map.
		Map<String, Object> emptyParams = Map.of();

		dispatch(notificationLog, template, user, emptyParams);
	}

	// ================================
	// Private helpers
	// ================================

	private GetUserDetailResponse fetchUserProfile(UUID userId) {
		return userGrpcClient.getUserDetail(GetUserDetailRequest.newBuilder()
				.setUserId(userId.toString())
				.build());
	}

	private String getUserLanguage(GetUserDetailResponse user) {
		String lang = user.getLanguage();
		return lang.isBlank() ? "en" : lang;
	}

	private NotificationTemplate findTemplateForUser(NotificationTemplateKey key,
	                                                 NotificationChannel channel,
	                                                 String userLang) {
		// First try user's language
		Optional<NotificationTemplate> template = templateRepository
				.findByTemplateKeyAndChannelAndLanguageAndActiveTrue(key, channel, userLang);
		if (template.isPresent()) {
			return template.get();
		}
		// Fallback to English
		template = templateRepository
				.findByTemplateKeyAndChannelAndLanguageAndActiveTrue(key, channel, "en");
		if (template.isPresent()) {
			log.info("Falling back to English template for {}/{}", key, channel);
			return template.get();
		}
		throw new InvalidRequestException("Invalid: template key: " + key + ", channel: " + channel + ", userLang:" + userLang);
	}

	private boolean isChannelSupported(NotificationChannel channel, GetUserDetailResponse user) {
		return switch (channel) {
			case EMAIL -> user.getEmail() != null && !user.getEmail().isBlank();
			case SMS -> user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank();
			case IN_APP -> userSessionRegistry.isUserOnline(user.getUserId()) ||
					(user.getFcmTokensList() != null && !user.getFcmTokensList().isEmpty());
			default -> false;
		};
	}

	private boolean isChannelEnabled(UUID userId,
	                                 NotificationTemplateKey templateKey,
	                                 NotificationChannel channel) {
		String category = getPreferenceCategory(templateKey);
		return preferenceRepository
				.findByUserIdAndNotificationTypeAndChannel(userId, category, channel)
				.map(UserNotificationPreference::isEnabled)
				.orElse(true); // default enabled
	}

	private String getPreferenceCategory(NotificationTemplateKey key) {
		return switch (key) {
			case USER_REGISTER, USER_VERIFICATION, USER_LOGIN, PASSWORD_RESET, MFA_CODE,
			     ACCOUNT_LOCKED, ADMIN_USER_CREATED, ADMIN_USER_BLOCKED,
			     ADMIN_USER_UNBLOCKED, ADMIN_USER_DELETED -> "ACCOUNT";
			case BOOKING_CREATED, BOOKING_REMINDER, BOOKING_CANCELED, BOOKING_COMPLETED,
			     MANAGER_BOOKING_ALERT, MANAGER_BOOKING_CANCEL, MANAGER_LOW_AVAILABILITY -> "BOOKING";
			case ORDER_CREATED -> "ORDER";
			case PAYMENT_SUCCESS, PAYMENT_FAILED, PAYMENT_REFUNDED -> "PAYMENT";
			case REVIEW_REQUEST, REVIEW_APPROVED, REVIEW_REJECTED -> "REVIEW";
		};
	}

	private NotificationLog prepareLogEntry(UUID eventId,
	                                        UUID userId,
	                                        NotificationChannel channel,
	                                        NotificationTemplate template,
	                                        GetUserDetailResponse user) {
		return NotificationLog.builder()
				.eventId(eventId)
				.userId(userId)
				.channel(channel)
				.templateKey(template.getTemplateKey().name())
				.templateId(template.getId())
				.recipient(getRecipient(channel, user))
				.status(NotificationStatus.PENDING)
				.priority(NotificationPriority.MEDIUM)
				.retryCount(0)
				.build();
	}

	private String getRecipient(NotificationChannel channel, GetUserDetailResponse user) {
		return switch (channel) {
			case EMAIL -> user.getEmail();
			case SMS -> user.getPhoneNumber();
			case IN_APP -> user.getUserId(); // for WebSocket routing
			default -> throw new IllegalArgumentException("Unsupported channel: " + channel);
		};
	}

	private void dispatch(NotificationLog notificationLog,
	                      NotificationTemplate template,
	                      GetUserDetailResponse user,
	                      Map<String, Object> params) {
		try {
			// Render content once – reuse for both HTML and plain text
			String renderedHtml = templateService.generateHtmlContent(template.getContent(), params);
			String renderedText = stripHtml(renderedHtml);

			switch (notificationLog.getChannel()) {
				case EMAIL -> {
					emailProvider.sendHtmlEmail(user.getEmail(), template.getTitle(), renderedHtml, notificationLog.getId());
					notificationLog.setStatus(NotificationStatus.SENT);
				}
				case SMS -> {
					if (!smsProvider.isLocallyValid(user.getPhoneNumber())) {
						throw new IllegalArgumentException("Invalid phone number format");
					}
					smsProvider.send(user.getPhoneNumber(), renderedText);
					notificationLog.setStatus(NotificationStatus.SENT);
				}
				case IN_APP -> {
					boolean delivered = false;
					// WebSocket for active session
					if (userSessionRegistry.isUserOnline(user.getUserId())) {
						inAppProvider.sendRealTimeUpdate(user.getUserId(), template.getTitle(), renderedHtml);
						delivered = true;
					}
					// Push notifications to all registered FCM tokens
					List<String> tokens = user.getFcmTokensList();
					if (!tokens.isEmpty()) {
						for (String token : tokens) {
							try {
								pushProvider.sendPush(token, template.getTitle(), renderedHtml);
								delivered = true;
							} catch (Exception pushEx) {
								if (isTokenInvalid(pushEx)) {
									log.info("Invalid FCM token {} for user {}, reporting to User Service", token, user.getUserId());
									userGrpcClient.deleteDeviceToken(DeleteDeviceTokenRequest.newBuilder()
											.setFcmToken(token)
											.build());
								} else {
									log.warn("Push failed for token {}: {}", token, pushEx.getMessage());
								}
							}
						}
					}
					if (!delivered) {
						throw new IllegalStateException("User offline and no valid FCM tokens");
					}
					notificationLog.setStatus(NotificationStatus.SENT);
				}
			}
			notificationLog.setSentAt(OffsetDateTime.now());
		} catch (Exception e) {
			log.error("Dispatch failed for log {} (retry {}): {}",
					notificationLog.getId(), notificationLog.getRetryCount(), e.getMessage());
			notificationLog.setStatus(NotificationStatus.FAILED);
			notificationLog.setProviderResponse(Map.of("error", e.getMessage()));
		} finally {
			logRepository.save(notificationLog);
		}
	}

	private String stripHtml(String html) {
		if (html == null) return "";
		return html.replaceAll("<[^>]*>", " ").replaceAll("&[a-zA-Z]+;", " ").trim();
	}

	private boolean isTokenInvalid(Exception e) {
		String msg = e.getMessage().toLowerCase();
		return msg.contains("not-registered") ||
				msg.contains("invalid-registration-token") ||
				msg.contains("not-found");
	}
}