package com.tuttime.pos.controller;

import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.pos.entity.PosSystemConfig;
import com.tuttime.pos.repository.PosSystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class PosWebhookController {

	private static final Logger logger = LoggerFactory.getLogger(PosWebhookController.class);

	private final PosSystemConfigRepository posSystemConfigRepository;

	// Example for iiko webhook endpoint
	@PostMapping("/iiko")
	public ResponseEntity<String> receiveIikoWebhook(@RequestBody String payload,
	                                                 @RequestHeader(name = "X-Signature", required = false) String signature) {

		return processWebhook("iiko", payload, signature);
	}

	// Example for rkeeper webhook endpoint
	@PostMapping("/rkeeper")
	public ResponseEntity<String> receiveRKeeperWebhook(@RequestBody String payload,
	                                                    @RequestHeader(name = "X-Signature", required = false) String signature) {

		return processWebhook("rkeeper", payload, signature);
	}

	private ResponseEntity<String> processWebhook(String posSystem, String payload, String signature) {
		PosSystemConfig config = posSystemConfigRepository.findByPosSystemAndEnabledTrue(posSystem)
				.orElseThrow(() -> new MethodArgumentNotValidException(
						"Webhook received for unknown or disabled POS system: " + posSystem
				));

		if (!validateSignature(payload, signature, config.getWebhookSecret())) {
			logger.warn("Invalid webhook signature for POS system: {}", posSystem);
			return ResponseEntity.status(401).body("Invalid signature");
		}

		// TODO: Process payload, parse JSON, map to events, update DB, publish Kafka events, etc.
		logger.info("Received valid webhook from POS system: {}", posSystem);
		return ResponseEntity.ok("Webhook processed");
	}

	private boolean validateSignature(String payload, String signature, String secret) {
		if (!StringUtils.hasText(signature) || !StringUtils.hasText(secret)) {
			return false;
		}
		// Example HMAC SHA256 validation, adjust according to POS docs
		String computedSignature = "sha256=" + Hex.encodeHexString(HmacUtils.hmacSha256(secret, payload));
		return computedSignature.equals(signature);
	}
}
