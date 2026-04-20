package com.tuttime.payment.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.payment.dto.PaymentDto;
import com.tuttime.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management")
public class PaymentController {

	private final PaymentService paymentService;

	/**
	 * NEW: Door for REST clients (Web/Admin) to create a payment session.
	 */
	@PostMapping("/checkout")
	@Operation(summary = "Create a Stripe Checkout Session via REST")
	public ResponseEntity<ApiRes<String>> createSession(
			@Valid @RequestBody PaymentDto.PaymentRequest request) {
		String url = paymentService.createCheckoutSession(
				request.bookingId(),
				request.userId(),
				request.branchId(),
				request.amount()
		);
		return ResponseEntity.ok(ApiRes.success(url));
	}

	@PostMapping("/{bookingId}/refund")
	public ResponseEntity<ApiRes<Void>> refund(
			@PathVariable UUID bookingId,
			@RequestParam BigDecimal amount) {
		paymentService.processRefund(bookingId, amount, "Admin Request");
		return ResponseEntity.ok(ApiRes.success(null));
	}

	@PostMapping("/test/confirm-booking/{bookingId}")
	@Operation(summary = "FOR TESTING: Manually confirms a payment and triggers Kafka event")
	public ResponseEntity<ApiRes<String>> testConfirmPayment(
			@PathVariable UUID bookingId) {
		// In dev mode, we skip Stripe and force the 'PAID' state
		String mockStripeId = "test_pi_" + UUID.randomUUID().toString();
		paymentService.testManualPaymentSuccess(bookingId, mockStripeId);

		return ResponseEntity.ok(ApiRes.success("Success: Kafka event sent for booking " + bookingId));
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
