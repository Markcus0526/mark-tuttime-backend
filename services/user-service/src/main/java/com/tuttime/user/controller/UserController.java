package com.tuttime.user.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.user.dto.*;
import com.tuttime.user.entity.enums.UserStatus;
import com.tuttime.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Operations for managing users, roles, authentication, and verification")
public class UserController {

	private final UserService userService;

	@Operation(summary = "Create a new user", description = "Creates a new user with phone number or email and optional profile details")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "User created successfully",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = UserResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
			@ApiResponse(responseCode = "409", description = "User with phone or email already exists", content = @Content)
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserResponse>> createUser(
			@Parameter(description = "User details to create", required = true)
			@Valid @RequestBody UserCreateRequest request) {
		if (!(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		UserResponse createdUser = userService.createUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(createdUser));
	}

	@Operation(summary = "Get user by ID", description = "Fetches a user by their unique identifier (UUID)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User found",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = UserResponse.class))),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserResponse>> getUserById(
			@Parameter(description = "UUID of the user to retrieve", required = true, in = ParameterIn.PATH)
			@PathVariable UUID id) {
		if (!(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		UserResponse user = userService.getUserById(id);
		return ResponseEntity.ok(ApiRes.success(user));
	}

	@Operation(summary = "Update user details", description = "Updates user information partially by user ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User updated successfully",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = UserResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid update data", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserResponse>> updateUser(
			@Parameter(description = "UUID of the user to update", required = true, in = ParameterIn.PATH)
			@PathVariable UUID id,
			@Parameter(description = "Fields to update", required = true)
			@Valid @RequestBody UserUpdateRequest request) {
		if (!(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		UserResponse updatedUser = userService.updateUser(id, request);
		return ResponseEntity.ok(ApiRes.success(updatedUser));
	}

	@Operation(summary = "Delete user", description = "Marks a user as deleted without removing from database")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "User soft deleted successfully"),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiRes<Void>> deleteUser(
			@Parameter(description = "UUID of the user to soft delete", required = true, in = ParameterIn.PATH)
			@PathVariable UUID id) {
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "List users", description = "Lists users with optional inclusion of soft deleted users, pagination, and sorting")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of users",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = UserResponse.class)))
	})
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Page<UserResponse>>> listUsers(
			@Parameter(description = "Include blocked, deleted, pending request users in results", required = false)
			@RequestParam(required = false) UserStatus status,

			@Parameter(description = "Include ADMIN, OWNER, MANAGER... users in results", required = false)
			@RequestParam(required = false) String roleName,

			@Parameter(description = "Page number (0-based)", required = false)
			@RequestParam(defaultValue = "0") int page,

			@Parameter(description = "Page size", required = false)
			@RequestParam(defaultValue = "20") int size,

			@Parameter(description = "Sort by field", required = false)
			@RequestParam(defaultValue = "id") String sort,

			@Parameter(description = "Sort direction: asc or desc", required = false)
			@RequestParam(defaultValue = "asc") String direction) {
		if (!(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		Page<UserResponse> users = userService.listUsers(status, roleName, page, size, sort, direction);
		return ResponseEntity.ok(ApiRes.success(users));
	}

	@Operation(summary = "Assign role to user", description = "Assigns a role to a user by user ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Role assigned successfully"),
			@ApiResponse(responseCode = "404", description = "User or role not found", content = @Content)
	})
	@PostMapping(value = "/{id}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Void>> assignRoleToUser(
			@Parameter(description = "UUID of user", required = true, in = ParameterIn.PATH)
			@PathVariable UUID id,
			@Parameter(description = "Role to assign", required = true)
			@RequestBody RoleAssignRequest request) {
		if (!(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		userService.assignRoleToUser(id, request.getRoleName());
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(summary = "Remove role from user", description = "Removes a role from the user")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Role removed successfully"),
			@ApiResponse(responseCode = "404", description = "User or role not found", content = @Content)
	})
	@DeleteMapping("/{id}/roles/{roleName}")
	public ResponseEntity<ApiRes<Void>> removeRoleFromUser(
			@Parameter(description = "UUID of user", required = true, in = ParameterIn.PATH)
			@PathVariable UUID id,
			@Parameter(description = "Role name to remove", required = true, in = ParameterIn.PATH)
			@PathVariable String roleName) {
		if (!(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		userService.removeRoleFromUser(id, roleName);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Add authentication method", description = "Adds an authentication method (password or OAuth) for user")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Authentication method added successfully"),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content),
			@ApiResponse(responseCode = "409", description = "Auth method already exists", content = @Content)
	})
	@PostMapping(value = "/{id}/auth-methods", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Void>> addAuthMethod(
			@Parameter(description = "UUID of user", required = true, in = ParameterIn.PATH)
			@PathVariable UUID id,
			@Parameter(description = "Authentication method details", required = true)
			@Valid @RequestBody AuthMethodCreateRequest request) {
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		userService.addAuthMethod(id, request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(summary = "Verify phone code", description = "Verifies a phone number using a verification code")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Phone number verified successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid or expired verification code", content = @Content)
	})
	@PostMapping(value = "/verify-phone", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Void>> verifyPhoneCode(
			@Parameter(description = "Phone verification details", required = true)
			@Valid @RequestBody OtpVerificationConfirmRequest request) {
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		userService.verifyPhoneCode(request.getPhoneNumber(), request.getCode());
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Request password reset", description = "Requests a password reset token by email or phone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Password reset token sent if user exists"),
			@ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content)
	})
	@PostMapping(value = "/password-reset/request", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Void>> requestPasswordReset(
			@Parameter(description = "Email or phone to send password reset token", required = true)
			@RequestBody PasswordResetRequest request) {
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		userService.requestPasswordReset(request.getEmailOrPhone());
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Confirm password reset", description = "Resets password using a valid reset token")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Password reset successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid token or password", content = @Content),
			@ApiResponse(responseCode = "404", description = "Token not found", content = @Content)
	})
	@PostMapping(value = "/password-reset/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Void>> confirmPasswordReset(
			@Parameter(description = "Password reset confirmation data", required = true)
			@Valid @RequestBody PasswordResetConfirmRequest request) {
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific user"));
		}

		userService.resetPassword(request.getToken(), request.getNewPassword());
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Update user location", description = "Saves user geolocation data")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "User location updated successfully"),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@PostMapping(value = "/{id}/location", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Void>> updateUserLocation(
			@Parameter(description = "UUID of user", required = true, in = ParameterIn.PATH)
			@PathVariable UUID id,
			@Parameter(description = "User location data", required = true)
			@Valid @RequestBody UserLocationRequest userLocationRequest) {
		userService.updateUserLocation(id, userLocationRequest);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(summary = "Block user", description = "Blocks a user's access to the system. Triggers real-time rejection at the Gateway level.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User blocked successfully",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = UserResponse.class))),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@PatchMapping(value = "/{id}/block", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserResponse>> blockUser(
			@Parameter(description = "UUID of the user to block", required = true, in = ParameterIn.PATH)
			@PathVariable UUID id) {

		// Perfect security check using your UserContext pattern
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ApiRes.error("Only administrators can block users"));
		}

		UserResponse blockedUser = userService.blockUser(id);
		return ResponseEntity.ok(ApiRes.success(blockedUser));
	}

	@Operation(summary = "Unblock user", description = "Restores access for a previously blocked user.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User unblocked successfully",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = UserResponse.class))),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@PatchMapping(value = "/{id}/unblock", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserResponse>> unblockUser(
			@Parameter(description = "UUID of the user to unblock", required = true, in = ParameterIn.PATH)
			@PathVariable UUID id) {

		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ApiRes.error("Only administrators can unblock users"));
		}

		UserResponse unblockedUser = userService.unblockUser(id);
		return ResponseEntity.ok(ApiRes.success(unblockedUser));
	}

}
