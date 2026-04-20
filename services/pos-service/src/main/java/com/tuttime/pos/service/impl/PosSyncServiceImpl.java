package com.tuttime.pos.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.pos.dto.*;
import com.tuttime.pos.entity.PosLog;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.entity.PosSystemConfig;
import com.tuttime.pos.entity.SyncLog;
import com.tuttime.pos.mapper.PosMapper;
import com.tuttime.pos.repository.PosLogRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import com.tuttime.pos.repository.PosSystemConfigRepository;
import com.tuttime.pos.repository.SyncLogRepository;
import com.tuttime.pos.service.PosSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosSyncServiceImpl implements PosSyncService {

	private final PosSystemConfigRepository posSystemConfigRepository;

	@Override
	public ResponseEntity<String> processWebhook(String posProvider, String payload, String signature) {
		PosSystemConfig config = posSystemConfigRepository.findByPosSystemAndEnabledTrue(posProvider)
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
