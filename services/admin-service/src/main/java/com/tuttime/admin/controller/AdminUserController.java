package com.tuttime.admin.controller;

import com.tuttime.admin.dto.BlockedUserResponse;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminUserService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin User Management Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "User management APIs for admins")
public class AdminUserController {

	private final AdminUserService userService;
	private final AdminAuditService auditService;

	@GetMapping("/blocklist")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List all blocked users", description = "Get paginated list of all blocked users with reasons")
	public ResponseEntity<ApiResponse<Page<BlockedUserResponse>>> listBlockedUsers(
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<BlockedUserResponse> users = userService.listBlockedUsers(pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_BLOCKLIST_VIEWED,
				"USER", null, "Listed blocked users");

		return ResponseEntity.ok(ApiResponse.success("Blocked users retrieved successfully", users));
	}
}

