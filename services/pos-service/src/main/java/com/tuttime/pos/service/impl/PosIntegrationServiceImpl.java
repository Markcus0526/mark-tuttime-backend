package com.tuttime.pos.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.service.PosIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosIntegrationServiceImpl implements PosIntegrationService {

	private final PosConfigRepository posConfigRepository;

	@Override
	public ResponseEntity<String> processWebhook(String posProvider, String payload, String signature) {
		PosConfig config = posConfigRepository.findByPosSystemAndEnabledTrue(posProvider)
				.orElseThrow(() -> new InvalidRequestException(
						"Webhook received for unknown or disabled POS system: " + posProvider
				));

		if (!validateSignature(payload, signature, config.getWebhookSecret())) {
			log.warn("Invalid webhook signature for POS system: {}", posProvider);
			return ResponseEntity.status(401).body("Invalid signature");
		}

		// TODO: Process payload, parse JSON, map to events, update DB, publish Kafka events, etc.
		log.info("Received valid webhook from POS system: {}", posProvider);
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
