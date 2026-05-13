package com.tuttime.booking.controller;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.dto.WaitlistDto;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/booking/waitlist")
@RequiredArgsConstructor
@Tag(name = "Waitlist Management", description = "Endpoints for managing the virtual queue when tables are full")
public class WaitlistController {

	private final BookingService bookingService;

	@PostMapping
	@Operation(summary = "Join the waitlist", description = "Adds a user to the virtual queue for a specific branch.")
	public ResponseEntity<ApiRes<WaitlistDto.WaitlistResponse>> joinWaitlist(
			@Valid @RequestBody WaitlistDto.WaitlistRequest request) {

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		WaitlistDto.WaitlistResponse data = bookingService.addToWaitlist(request, currentUserId, userRoles);

		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(data.id())
				.toUri();

		return ResponseEntity.created(location).body(ApiRes.success(data));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get waitlist status", description = "Check current position and estimated wait time.")
	public ResponseEntity<ApiRes<WaitlistDto.WaitlistResponse>> getWaitlistStatus(@PathVariable UUID id) {
		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		WaitlistDto.WaitlistResponse data = bookingService.getWaitlistEntry(id, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Leave the waitlist", description = "Cancels the waitlist entry (soft delete/status update).")
	public ResponseEntity<ApiRes<Void>> leaveWaitlist(@PathVariable UUID id) {

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		bookingService.leaveWaitlist(id, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@PostMapping("/{id}/claim")
	@Operation(summary = "Claim a notified waitlist entry and create a booking")
	public ResponseEntity<ApiRes<BookingDto.BookingResponse>> claimWaitlist(
			@PathVariable UUID id,
			@Valid @RequestBody BookingDto.BookingRequest bookingRequest) {

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		BookingDto.BookingResponse booking =
				bookingService.claimWaitlistEntry(id, bookingRequest, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(booking));
	}

	// -------------------- Helper methods --------------------
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