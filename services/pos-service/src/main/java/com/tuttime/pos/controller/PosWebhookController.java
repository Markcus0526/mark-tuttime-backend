package com.tuttime.pos.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.pos.service.PosIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class PosWebhookController {

	private final PosIntegrationService posIntegrationService;

	@PostMapping("/webhook/{posProvider}")
	@Operation(summary = "POS Webhook Receiver", description = "Receives real-time status updates from IIKO/RKEEPER.")
	public ResponseEntity<Void> handlePosWebhook(
			@PathVariable String posProvider,
			@RequestHeader(name = "X-POS-Signature", required = false) String signature,
			@RequestBody String payload) {

		// Webhooks are usually authenticated via signature, not UserContext
		posIntegrationService.processWebhook(posProvider, signature, payload);
		return ResponseEntity.ok().build();
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
