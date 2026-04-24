package com.tuttime.notification.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
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
	public ResponseEntity<ApiRes<List<NotificationLog>>> getMyNotifications() { // Or use @AuthenticationPrincipal

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

		List<NotificationLog> notifications = userNotificationService.getUserNotifications(currentUserId);
		return ResponseEntity.ok(ApiRes.success("Notifications retrieved successfully", notifications));
	}

	@Operation(summary = "Mark a notification as read")
	@PatchMapping("/{id}/read")
	public ResponseEntity<ApiRes<Void>> markAsRead(@PathVariable UUID id) {

		validateLogin();

		userNotificationService.updateStatus(id, NotificationStatus.DELIVERED);
		return ResponseEntity.ok(ApiRes.success("Notification marked as read", null));
	}

	@Operation(summary = "Clear all my notifications")
	@DeleteMapping("/clear")
	public ResponseEntity<ApiRes<Void>> clearAll() {

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

		userNotificationService.clearAll(currentUserId);
		return ResponseEntity.ok(ApiRes.success("Inbox cleared", null));
	}

	/**
	 * Private helper to keep the controller DRY
	 */
	private void validateLogin() {
		if (!UserContext.isValidUUID()) {
			throw new UnauthorizedException("Unauthorized: You should login");
		}
	}

	private void validateAdmin() {
		if (!UserContext.isValidUUID() || !UserContext.isRoleAdmin()) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}
}
