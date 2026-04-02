package com.tuttime.payment.controller;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.payment.dto.PaymentRequest;
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
	public ResponseEntity<ApiRes<String>> createSession(@Valid @RequestBody PaymentRequest request) {
		String url = paymentService.createCheckoutSession(
				request.bookingId(),
				request.userId(),
				request.amount()
		);
		return ResponseEntity.ok(ApiRes.success(url));
	}

	@PostMapping("/{id}/refund")
	public ResponseEntity<ApiRes<Void>> refund(@PathVariable UUID id, @RequestParam BigDecimal amount) {
		paymentService.processRefund(id, amount, "Admin Request");
		return ResponseEntity.ok(ApiRes.success(null));
	}
}
