package com.tuttime.notification.controller;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.notification.entity.NotificationLog;
import com.tuttime.notification.entity.enums.NotificationStatus;
import com.tuttime.notification.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/me")
@RequiredArgsConstructor
@Tag(name = "2.User(In-App) Notifications", description = "Personal notification inbox for staff and users")
public class UserNotificationController {

	private final UserNotificationService userNotificationService;

	@Operation(summary = "Get my in-app notifications", description = "Retrieves the recent notification history for the logged-in user.")
	@GetMapping
	public ResponseEntity<ApiRes<List<NotificationLog>>> getMyNotifications(
			@RequestHeader("X-User-Id") UUID userId) { // Or use @AuthenticationPrincipal

		List<NotificationLog> notifications = userNotificationService.getUserNotifications(userId);
		return ResponseEntity.ok(ApiRes.success("Notifications retrieved successfully", notifications));
	}

	@Operation(summary = "Mark a notification as read")
	@PatchMapping("/{id}/read")
	public ResponseEntity<ApiRes<Void>> markAsRead(@PathVariable UUID id) {
		userNotificationService.updateStatus(id, NotificationStatus.DELIVERED);
		return ResponseEntity.ok(ApiRes.success("Notification marked as read", null));
	}

	@Operation(summary = "Clear all my notifications")
	@DeleteMapping("/clear")
	public ResponseEntity<ApiRes<Void>> clearAll(@RequestHeader("X-User-Id") UUID userId) {
		userNotificationService.clearAll(userId);
		return ResponseEntity.ok(ApiRes.success("Inbox cleared", null));
	}
}
