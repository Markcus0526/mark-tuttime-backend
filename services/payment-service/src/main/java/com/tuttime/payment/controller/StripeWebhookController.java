package com.tuttime.payment.controller;

import com.tuttime.payment.service.StripeWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
@Tag(name = "Strip Payment Webhook")
public class StripeWebhookController {

	private final StripeWebhookService webhookService;

	@PostMapping("/stripe")
	public ResponseEntity<String> handleStripe(@RequestBody String payload,
	                                           @RequestHeader("Stripe-Signature") String sig) {
		// Signature verification requires the raw payload string
		webhookService.handleWebhook(payload, sig);
		return ResponseEntity.ok("Received");
	}
}