package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.NotificationRequest;
import com.tuttime.admin.dto.NotificationResponse;
import com.tuttime.admin.rest.client.NotificationServiceRestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminNotificationServiceImpl Tests")
class AdminNotificationServiceImplTest {

	@Mock
	private NotificationServiceRestClient notificationServiceRestClient;

	@InjectMocks
	private AdminNotificationServiceImpl adminNotificationService;

	@Test
	@DisplayName("Should list notifications successfully")
	void testListNotifications_Success_ReturnsPage() {
		// Given
		PageRequest pageable = PageRequest.of(0, 10);
		Map<String, Object> notificationMap = new HashMap<>();
		notificationMap.put("notificationId", 1L);
		notificationMap.put("userId", 100L);
		notificationMap.put("notificationType", "SYSTEM_ALERT");
		notificationMap.put("channel", "IN_APP");
		notificationMap.put("message", "Test message");
		notificationMap.put("status", "PENDING");
		notificationMap.put("priority", "NORMAL");

		Map<String, Object> pageData = new HashMap<>();
		pageData.put("content", Arrays.asList(notificationMap));
		pageData.put("totalElements", 1L);
		pageData.put("totalPages", 1);
		pageData.put("number", 0);
		pageData.put("size", 10);

		when(notificationServiceRestClient.listNotifications(pageable, null)).thenReturn(pageData);

		// When
		Page<NotificationResponse> result = adminNotificationService.listNotifications(pageable, null);

		// Then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getNotificationId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("Should get notification by ID successfully")
	void testGetNotification_Success_ReturnsNotification() {
		// Given
		Long id = 1L;
		Map<String, Object> notificationMap = new HashMap<>();
		notificationMap.put("notificationId", id);
		notificationMap.put("userId", 100L);
		notificationMap.put("notificationType", "SYSTEM_ALERT");
		notificationMap.put("channel", "IN_APP");
		notificationMap.put("message", "Test message");
		notificationMap.put("status", "PENDING");
		notificationMap.put("priority", "NORMAL");

		when(notificationServiceRestClient.getNotificationById(id)).thenReturn(notificationMap);

		// When
		NotificationResponse result = adminNotificationService.getNotification(id);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getNotificationId()).isEqualTo(id);
		assertThat(result.getUserId()).isEqualTo(100L);
	}

	@Test
	@DisplayName("Should create notification successfully")
	void testCreateNotification_Success_ReturnsNotification() {
		// Given
		NotificationRequest request = NotificationRequest.builder()
				.notificationType("SYSTEM_ALERT")
				.channel("IN_APP")
				.subject("Test Subject")
				.message("Test message")
				.priority("NORMAL")
				.userId(100L)
				.build();

		Map<String, Object> notificationMap = new HashMap<>();
		notificationMap.put("notificationId", 1L);
		notificationMap.put("userId", 100L);
		notificationMap.put("notificationType", "SYSTEM_ALERT");
		notificationMap.put("channel", "IN_APP");
		notificationMap.put("subject", "Test Subject");
		notificationMap.put("message", "Test message");
		notificationMap.put("status", "PENDING");
		notificationMap.put("priority", "NORMAL");

		when(notificationServiceRestClient.createNotification(any(Map.class))).thenReturn(notificationMap);

		// When
		NotificationResponse result = adminNotificationService.createNotification(request);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getNotificationId()).isEqualTo(1L);
		assertThat(result.getUserId()).isEqualTo(100L);
		verify(notificationServiceRestClient).createNotification(any(Map.class));
	}

	@Test
	@DisplayName("Should create scheduled notification successfully")
	void testCreateNotification_Scheduled_Success() {
		// Given
		OffsetDateTime scheduledAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
		NotificationRequest request = NotificationRequest.builder()
				.notificationType("PROMOTIONAL_OFFER")
				.channel("EMAIL")
				.subject("Special Offer")
				.message("Check out our offer!")
				.priority("HIGH")
				.userId(200L)
				.scheduledAt(scheduledAt)
				.build();

		Map<String, Object> notificationMap = new HashMap<>();
		notificationMap.put("notificationId", 2L);
		notificationMap.put("userId", 200L);
		notificationMap.put("notificationType", "PROMOTIONAL_OFFER");
		notificationMap.put("channel", "EMAIL");
		notificationMap.put("scheduledAt", scheduledAt.toString());
		notificationMap.put("status", "PENDING");

		when(notificationServiceRestClient.createNotification(any(Map.class))).thenReturn(notificationMap);

		// When
		NotificationResponse result = adminNotificationService.createNotification(request);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getScheduledAt()).isEqualTo(scheduledAt);
	}

	@Test
	@DisplayName("Should update notification successfully")
	void testUpdateNotification_Success_ReturnsUpdated() {
		// Given
		Long id = 1L;
		NotificationRequest request = NotificationRequest.builder()
				.notificationType("UPDATED_TYPE")
				.channel("EMAIL")
				.message("Updated message")
				.priority("HIGH")
				.build();

		Map<String, Object> notificationMap = new HashMap<>();
		notificationMap.put("notificationId", id);
		notificationMap.put("notificationType", "UPDATED_TYPE");
		notificationMap.put("channel", "EMAIL");
		notificationMap.put("message", "Updated message");
		notificationMap.put("priority", "HIGH");

		when(notificationServiceRestClient.updateNotification(eq(id), any(Map.class))).thenReturn(notificationMap);

		// When
		NotificationResponse result = adminNotificationService.updateNotification(id, request);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getNotificationType()).isEqualTo("UPDATED_TYPE");
		verify(notificationServiceRestClient).updateNotification(eq(id), any(Map.class));
	}

	@Test
	@DisplayName("Should delete notification successfully")
	void testDeleteNotification_Success() {
		// Given
		Long id = 1L;
		doNothing().when(notificationServiceRestClient).deleteNotification(id);

		// When
		adminNotificationService.deleteNotification(id);

		// Then
		verify(notificationServiceRestClient).deleteNotification(id);
	}

	@Test
	@DisplayName("Should publish notification successfully")
	void testPublishNotification_Success() {
		// Given
		Long id = 1L;
		Map<String, Object> notificationMap = new HashMap<>();
		notificationMap.put("notificationId", id);
		notificationMap.put("status", "PENDING");

		when(notificationServiceRestClient.publishNotification(id)).thenReturn(notificationMap);

		// When
		NotificationResponse result = adminNotificationService.publishNotification(id);

		// Then
		assertThat(result).isNotNull();
		verify(notificationServiceRestClient).publishNotification(id);
	}

	@Test
	@DisplayName("Should cancel notification successfully")
	void testCancelNotification_Success() {
		// Given
		Long id = 1L;
		Map<String, Object> notificationMap = new HashMap<>();
		notificationMap.put("notificationId", id);
		notificationMap.put("status", "CANCELLED");

		when(notificationServiceRestClient.cancelNotification(id)).thenReturn(notificationMap);

		// When
		NotificationResponse result = adminNotificationService.cancelNotification(id);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo("CANCELLED");
		verify(notificationServiceRestClient).cancelNotification(id);
	}

	@Test
	@DisplayName("Should schedule notification successfully")
	void testScheduleNotification_Success() {
		// Given
		Long id = 1L;
		OffsetDateTime scheduledAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
		Map<String, Object> notificationMap = new HashMap<>();
		notificationMap.put("notificationId", id);
		notificationMap.put("scheduledAt", scheduledAt.toString());
		notificationMap.put("status", "PENDING");

		when(notificationServiceRestClient.scheduleNotification(id, scheduledAt))
				.thenReturn(notificationMap);

		// When
		NotificationResponse result = adminNotificationService.scheduleNotification(id, scheduledAt);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getScheduledAt()).isEqualTo(scheduledAt);
		verify(notificationServiceRestClient).scheduleNotification(id, scheduledAt);
	}
}

