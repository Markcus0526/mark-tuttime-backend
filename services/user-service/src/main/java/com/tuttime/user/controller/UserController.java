package com.tuttime.user.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.user.dto.UserDto;
import com.tuttime.user.entity.enums.UserStatus;
import com.tuttime.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
			@ApiResponse(responseCode = "201", description = "User created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
			@ApiResponse(responseCode = "409", description = "User with phone or email already exists", content = @Content)
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserDto.UserResponse>> createUser(
			@Parameter(description = "User details to create", required = true)
			@Valid @RequestBody UserDto.UserCreateRequest request) {

		validateAdmin();

		UserDto.UserResponse createdUser = userService.createUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(createdUser));
	}

	@Operation(summary = "Get user by ID", description = "Fetches a user by their unique identifier (UUID)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User found"),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserDto.UserResponse>> getUserById(
			@Parameter(description = "UUID of the user to retrieve", required = true, in = ParameterIn.PATH)
			@PathVariable UUID userId) {

		// Allow Admin, Owner, and Manager to search/list users
		validateAdminOrOwnerOrManager();

		UserDto.UserResponse user = userService.getUserById(userId);
		return ResponseEntity.ok(ApiRes.success(user));
	}

	@Operation(summary = "Update user details", description = "Updates user information partially by user ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid update data", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@PatchMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserDto.UserResponse>> updateUser(
			@Parameter(description = "UUID of the user to update", required = true, in = ParameterIn.PATH)
			@PathVariable UUID userId,
			@Parameter(description = "Fields to update", required = true)
			@Valid @RequestBody UserDto.UserUpdateRequest request) {

		validateAdmin();

		UserDto.UserResponse updatedUser = userService.updateUser(userId, request);
		return ResponseEntity.ok(ApiRes.success(updatedUser));
	}

	@Operation(summary = "Delete user", description = "Marks a user as deleted without removing from database")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "User soft deleted successfully"),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@DeleteMapping("/{userId}")
	public ResponseEntity<ApiRes<Void>> deleteUser(
			@Parameter(description = "UUID of the user to soft delete", required = true, in = ParameterIn.PATH)
			@PathVariable UUID userId) {

		validateAdmin();

		userService.deleteUser(userId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "List users", description = "Lists users with optional inclusion of soft deleted users, pagination, and sorting")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of users")
	})
	@GetMapping
	public ResponseEntity<ApiRes<Page<UserDto.UserResponse>>> listAndSearchUsers(
			@Parameter(description = "Keyword to search (name, email, or phone)")
			@RequestParam(required = false) String query,

			@Parameter(description = "Include blocked, deleted, pending request users in results")
			@RequestParam(required = false) UserStatus status,

			@Parameter(description = "Include ADMIN, OWNER, MANAGER... users in results")
			@RequestParam(required = false) String roleName,

			@Parameter(description = "Page number (0-based)")
			@RequestParam(defaultValue = "0") int page,

			@Parameter(description = "Page size")
			@RequestParam(defaultValue = "20") int size,

			@Parameter(description = "Sort by field")
			@RequestParam(defaultValue = "id") String sort,

			@Parameter(description = "Sort direction: asc or desc")
			@RequestParam(defaultValue = "asc") String direction) {

		// Allow Admin, Owner, and Manager to search/list users
		validateAdminOrOwnerOrManager();

		Sort sortOrder = direction.equalsIgnoreCase("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending();
		Pageable pageable = PageRequest.of(page, size, sortOrder);

		// Call service with the optional query
		Page<UserDto.UserResponse> users = userService.findUsers(query, status, roleName, pageable);
		return ResponseEntity.ok(ApiRes.success(users));
	}

	@Operation(summary = "Assign role to user", description = "Assigns a role to a user by user ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Role assigned successfully"),
			@ApiResponse(responseCode = "404", description = "User or role not found", content = @Content)
	})
	@PostMapping(value = "/{userId}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Void>> assignRoleToUser(
			@Parameter(description = "UUID of user", required = true, in = ParameterIn.PATH)
			@PathVariable UUID userId,
			@Parameter(description = "Role to assign", required = true)
			@RequestBody UserDto.RoleAssignRequest request) {

		validateAdmin();

		userService.assignRoleToUser(userId, request.roleName());
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(summary = "Remove role from user", description = "Removes a role from the user")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Role removed successfully"),
			@ApiResponse(responseCode = "404", description = "User or role not found", content = @Content)
	})
	@DeleteMapping("/{userId}/roles/{roleName}")
	public ResponseEntity<ApiRes<Void>> removeRoleFromUser(
			@Parameter(description = "UUID of user", required = true, in = ParameterIn.PATH)
			@PathVariable UUID userId,
			@Parameter(description = "Role name to remove", required = true, in = ParameterIn.PATH)
			@PathVariable String roleName) {

		validateAdmin();

		userService.removeRoleFromUser(userId, roleName);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "Add authentication method", description = "Adds an authentication method (password or OAuth) for user")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Authentication method added successfully"),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content),
			@ApiResponse(responseCode = "409", description = "Auth method already exists", content = @Content)
	})
	@PostMapping(value = "/{userId}/auth-methods", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Void>> addAuthMethod(
			@Parameter(description = "UUID of user", required = true, in = ParameterIn.PATH)
			@PathVariable UUID userId,
			@Parameter(description = "Authentication method details", required = true)
			@Valid @RequestBody UserDto.AuthMethodCreateRequest request) {

		validateAdmin();

		userService.addAuthMethod(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(summary = "Update user location", description = "Saves user geolocation data")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "User location updated successfully"),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@PostMapping(value = "/{userId}/location", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<Void>> updateUserLocation(
			@Parameter(description = "UUID of user", required = true, in = ParameterIn.PATH)
			@PathVariable UUID userId,
			@Parameter(description = "User location data", required = true)
			@Valid @RequestBody UserDto.UserLocationRequest userLocationRequest) {

		validateLogin();

		userService.updateUserLocation(userId, userLocationRequest);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(summary = "Block user", description = "Blocks a user's access to the system. Triggers real-time rejection at the Gateway level.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User blocked successfully"),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@PatchMapping(value = "/{userId}/block", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserDto.UserResponse>> blockUser(
			@Parameter(description = "UUID of the user to block", required = true, in = ParameterIn.PATH)
			@PathVariable UUID userId) {

		validateAdmin();

		UserDto.UserResponse blockedUser = userService.blockUser(userId);
		return ResponseEntity.ok(ApiRes.success(blockedUser));
	}

	@Operation(summary = "Unblock user", description = "Restores access for a previously blocked user.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User unblocked successfully"),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content)
	})
	@PatchMapping(value = "/{userId}/unblock", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserDto.UserResponse>> unblockUser(
			@Parameter(description = "UUID of the user to unblock", required = true, in = ParameterIn.PATH)
			@PathVariable UUID userId) {

		validateAdmin();

		UserDto.UserResponse unblockedUser = userService.unblockUser(userId);
		return ResponseEntity.ok(ApiRes.success(unblockedUser));
	}

	@Operation(summary = "Update my profile", description = "Allows the logged-in user to update their name, profile picture, etc.")
	@PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiRes<UserDto.UserResponse>> updateProfile(@RequestBody @Valid UserDto.UserUpdateRequest request) {

		validateLogin();

		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		UserDto.UserResponse updatedUser = userService.updateUser(currentUserId, request);

		return ResponseEntity.ok(ApiRes.success(updatedUser));
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
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}
}
