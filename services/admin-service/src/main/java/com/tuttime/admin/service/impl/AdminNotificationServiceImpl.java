package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.NotificationRequest;
import com.tuttime.admin.dto.NotificationResponse;
import com.tuttime.admin.rest.client.NotificationServiceRestClient;
import com.tuttime.admin.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Notification Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

	private final NotificationServiceRestClient notificationServiceRestClient;

	@Override
	public Page<NotificationResponse> listNotifications(Pageable pageable, String status) {
		Map<String, Object> pageData = notificationServiceRestClient.listNotifications(pageable, status);

		// Convert Map to Page<NotificationResponse>
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.get("content");
		long totalElements = ((Number) pageData.get("totalElements")).longValue();

		List<NotificationResponse> responseList = content.stream()
				.map(this::mapToNotificationResponse)
				.collect(Collectors.toList());

		return new PageImpl<>(responseList, pageable, totalElements);
	}

	@Override
	public NotificationResponse getNotification(Long id) {
		Map<String, Object> notificationMap = notificationServiceRestClient.getNotificationById(id);
		return mapToNotificationResponse(notificationMap);
	}

	@Override
	public NotificationResponse createNotification(NotificationRequest request) {
		// Convert NotificationRequest to Map
		Map<String, Object> requestMap = convertRequestToMap(request);
		Map<String, Object> notificationMap = notificationServiceRestClient.createNotification(requestMap);
		return mapToNotificationResponse(notificationMap);
	}

	@Override
	public NotificationResponse updateNotification(Long id, NotificationRequest request) {
		// Convert NotificationRequest to Map
		Map<String, Object> requestMap = convertRequestToMap(request);
		Map<String, Object> notificationMap = notificationServiceRestClient.updateNotification(id, requestMap);
		return mapToNotificationResponse(notificationMap);
	}

	@Override
	public void deleteNotification(Long id) {
		notificationServiceRestClient.deleteNotification(id);
	}

	@Override
	public NotificationResponse publishNotification(Long id) {
		Map<String, Object> notificationMap = notificationServiceRestClient.publishNotification(id);
		return mapToNotificationResponse(notificationMap);
	}

	@Override
	public NotificationResponse cancelNotification(Long id) {
		Map<String, Object> notificationMap = notificationServiceRestClient.cancelNotification(id);
		return mapToNotificationResponse(notificationMap);
	}

	@Override
	public NotificationResponse scheduleNotification(Long id, OffsetDateTime scheduledAt) {
		Map<String, Object> notificationMap = notificationServiceRestClient.scheduleNotification(id, scheduledAt);
		return mapToNotificationResponse(notificationMap);
	}

	/**
	 * Convert Map to NotificationResponse
	 */
	private NotificationResponse mapToNotificationResponse(Map<String, Object> map) {
		if (map == null) {
			return null;
		}

		NotificationResponse.NotificationResponseBuilder builder = NotificationResponse.builder();

		if (map.get("notificationId") != null) {
			Object idObj = map.get("notificationId");
			builder.notificationId(idObj instanceof Number ? ((Number) idObj).longValue() : null);
		}
		if (map.get("userId") != null) {
			Object userIdObj = map.get("userId");
			builder.userId(userIdObj instanceof Number ? ((Number) userIdObj).longValue() : null);
		}
		if (map.get("notificationType") != null) {
			builder.notificationType(map.get("notificationType").toString());
		}
		if (map.get("channel") != null) {
			builder.channel(map.get("channel").toString());
		}
		if (map.get("subject") != null) {
			builder.subject(map.get("subject").toString());
		}
		if (map.get("message") != null) {
			builder.message(map.get("message").toString());
		}
		if (map.get("status") != null) {
			builder.status(map.get("status").toString());
		}
		if (map.get("priority") != null) {
			builder.priority(map.get("priority").toString());
		}
		if (map.get("referenceType") != null) {
			builder.referenceType(map.get("referenceType").toString());
		}
		if (map.get("referenceId") != null) {
			builder.referenceId(map.get("referenceId").toString());
		}
		// Parse datetime fields (assumed UTC timezone from API responses)
		if (map.get("scheduledAt") != null) {
			try {
				builder.scheduledAt(OffsetDateTime.parse(map.get("scheduledAt").toString()).withOffsetSameInstant(ZoneOffset.UTC));
			} catch (Exception e) {
				log.warn("Failed to parse scheduledAt: {}", map.get("scheduledAt"), e);
			}
		}
		if (map.get("sentAt") != null) {
			try {
				builder.sentAt(OffsetDateTime.parse(map.get("sentAt").toString()).withOffsetSameInstant(ZoneOffset.UTC));
			} catch (Exception e) {
				log.warn("Failed to parse sentAt: {}", map.get("sentAt"), e);
			}
		}
		if (map.get("createdAt") != null) {
			try {
				builder.createdAt(OffsetDateTime.parse(map.get("createdAt").toString()).withOffsetSameInstant(ZoneOffset.UTC));
			} catch (Exception e) {
				log.warn("Failed to parse createdAt: {}", map.get("createdAt"), e);
			}
		}
		if (map.get("updatedAt") != null) {
			try {
				builder.updatedAt(OffsetDateTime.parse(map.get("updatedAt").toString()).withOffsetSameInstant(ZoneOffset.UTC));
			} catch (Exception e) {
				log.warn("Failed to parse updatedAt: {}", map.get("updatedAt"), e);
			}
		}
		if (map.get("isRead") != null) {
			Object readObj = map.get("isRead");
			builder.isRead(readObj instanceof Boolean ? (Boolean) readObj : Boolean.parseBoolean(readObj.toString()));
		}
		if (map.get("readAt") != null) {
			try {
				builder.readAt(OffsetDateTime.parse(map.get("readAt").toString()).withOffsetSameInstant(ZoneOffset.UTC));
			} catch (Exception e) {
				log.warn("Failed to parse readAt: {}", map.get("readAt"), e);
			}
		}
		if (map.get("retryCount") != null) {
			Object retryObj = map.get("retryCount");
			builder.retryCount(retryObj instanceof Number ? ((Number) retryObj).intValue() : null);
		}
		if (map.get("errorMessage") != null) {
			builder.errorMessage(map.get("errorMessage").toString());
		}
		if (map.get("metadata") != null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> metadata = (Map<String, Object>) map.get("metadata");
			builder.metadata(metadata);
		}

		return builder.build();
	}

	/**
	 * Convert NotificationRequest to Map
	 */
	private Map<String, Object> convertRequestToMap(NotificationRequest request) {
		Map<String, Object> map = new HashMap<>();
		map.put("notificationType", request.getNotificationType());
		map.put("channel", request.getChannel());
		map.put("subject", request.getSubject());
		map.put("message", request.getMessage());
		map.put("priority", request.getPriority());
		map.put("userId", request.getUserId());
		map.put("userIds", request.getUserIds());
		map.put("scheduledAt", request.getScheduledAt());
		map.put("referenceType", request.getReferenceType());
		map.put("referenceId", request.getReferenceId());
		map.put("metadata", request.getMetadata());
		return map;
	}
}

