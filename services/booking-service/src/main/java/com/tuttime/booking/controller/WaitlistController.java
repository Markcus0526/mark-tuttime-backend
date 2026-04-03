package com.tuttime.booking.controller;

import com.tuttime.booking.dto.WaitlistDto;
import com.tuttime.booking.service.WaitlistService;
import com.tuttime.common.dto.ApiRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

	private final WaitlistService waitlistService;

	@PostMapping
	@Operation(summary = "Join the waitlist", description = "Adds a user to the virtual queue for a specific branch.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Joined successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request or user already on waitlist"),
			@ApiResponse(responseCode = "409", description = "User is already active in this queue")
	})
	public ResponseEntity<ApiRes<WaitlistDto.WaitlistResponse>> joinWaitlist(@Valid @RequestBody WaitlistDto.WaitlistRequest request) {
		WaitlistDto.WaitlistResponse data = waitlistService.addToWaitlist(request);

		// Perfect REST: Create a location URI for the new waitlist entry
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(data.id())
				.toUri();

		return ResponseEntity.created(location).body(ApiRes.success(data));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get waitlist status", description = "Check current position and estimated wait time.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "404", description = "Waitlist entry not found")
	})
	public ResponseEntity<ApiRes<WaitlistDto.WaitlistResponse>> getWaitlistStatus(@PathVariable UUID id) {
		WaitlistDto.WaitlistResponse data = waitlistService.getWaitlistEntry(id);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Leave the waitlist", description = "Cancels the waitlist entry (soft delete/status update).")
	@ApiResponse(responseCode = "204", description = "Successfully left the queue")
	public ResponseEntity<Void> leaveWaitlist(@PathVariable UUID id) {
		waitlistService.leaveWaitlist(id);
		return ResponseEntity.noContent().build();
	}
}
