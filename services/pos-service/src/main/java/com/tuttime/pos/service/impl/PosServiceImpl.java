package com.tuttime.pos.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.pos.dto.PosConfigDto;
import com.tuttime.pos.dto.PosSyncDto;
import com.tuttime.pos.dto.PosSyncLogDto;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.PosLog;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.mapper.PosMapper;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.repository.PosLogRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import com.tuttime.pos.service.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosServiceImpl implements PosService {

	private final PosConfigRepository posConfigRepository;
	private final PosLogRepository posLogRepository;
	private final PosSyncQueueRepository posSyncQueueRepository;
	private final PosMapper posMapper;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(readOnly = true)
	public PosSyncLogDto.PosSyncLogResponse getSyncLogs(UUID entityId) {
		// Fetch all logs for this entity (Order or Booking)
		List<PosLog> logs = posLogRepository.findAllByEntityIdOrderByCreatedAtDesc(entityId);
		if (logs.isEmpty()) {
			throw new InvalidRequestException("No sync history found for entity: " + entityId);
		}
		return posMapper.toResponse(entityId, logs);
	}

	@Override
	@Transactional
	public PosConfig saveConfig(PosConfigDto.PosConfigRequest request) {
		// 1. Find existing or create a new instance (The Upsert pattern)
		PosConfig config = posConfigRepository.findByBranchId(request.branchId())
				.orElseGet(() -> {
					PosConfig newConfig = new PosConfig();
					newConfig.setBranchId(request.branchId()); // Only set on creation
					return newConfig;
				});

		// 2. Update fields from the request
		config.setPosProvider(request.provider());
		config.setApiEndpoint(request.apiEndpoint());
		config.setApiKey(request.apiKey()); // Added based on v1.2 Schema
		config.setWebhookSecret(request.webhookSecret());
		config.setEnabled(request.enabled());

		// 3. Save (JPA will either UPDATE or INSERT automatically)
		return posConfigRepository.save(config);
	}

	@Override
	@Transactional
	public void syncOrder(PosSyncDto.OrderSyncRequest request) {
		log.info("Manually triggering sync for order: {}", request.orderId());

		// 1. Get POS Config based on the request (dynamic lookup)
		PosConfig config = posConfigRepository.findByBranchId(request.branchId())
				.orElseThrow(() -> new InvalidRequestException("POS Config not found for " + request.branchId()));

		Map<String, Object> payload = new HashMap<>();
		payload.put("order_id", request.orderId());
		payload.put("branch_id", request.branchId()); // Must be included!
		payload.put("sync_type", "MANUAL");

		// 1. Log the initiation
		posLogRepository.save(PosLog.builder()
				.branchId(request.branchId())
				.entityType("ORDER")
				.entityId(request.orderId())
				.status("PENDING")
				.requestPayload(payload)
				.build());

		// 2. Queue for processing (removed .posLog())
		posSyncQueueRepository.save(PosSyncQueue.builder()
				.branchId(request.branchId())
				.orderId(request.orderId()) // Added direct reference
				.action("SEND_ORDER")
				.payload(payload)
				.status("PENDING")
				.nextRetryAt(OffsetDateTime.now())
				.retryCount(0)
				.build());
	}

	// Stubbed methods for Booking and Menu (similar patterns to syncOrder)
	@Override
	@Transactional
	public void syncBooking(PosSyncDto.BookingSyncRequest request) {
		log.info("Queuing booking sync for ID: {} at branch: {}", request.bookingId(), request.branchId());

		PosConfig config = posConfigRepository.findByBranchId(request.branchId())
				.orElseThrow(() -> new InvalidRequestException("POS Config not found for " + request.branchId()));

		Map<String, Object> payload = new HashMap<>();
		payload.put("booking_id", request.bookingId());
		payload.put("branch_id", request.branchId());

		posLogRepository.save(PosLog.builder()
				.branchId(request.branchId())
				.entityType("BOOKING")
				.entityId(request.bookingId())
				.status("PENDING")
				.requestPayload(payload)
				.build());

		posSyncQueueRepository.save(PosSyncQueue.builder()
				.branchId(request.branchId())
				.bookingId(request.bookingId()) // Added direct reference
				.action("RESERVE_TABLE")
				.payload(payload)
				.status("PENDING")
				.nextRetryAt(OffsetDateTime.now())
				.retryCount(0)
				.build());
	}

	@Override
	@Transactional
	public void syncMenu(PosSyncDto.MenuSyncRequest request) {
		log.info("Initializing menu catalog pull for branch: {}", request.branchId());

		// 1. Verify config exists before doing anything
		posConfigRepository.findByBranchId(request.branchId())
				.orElseThrow(() -> new InvalidRequestException("POS Config not found for " + request.branchId()));

		// 2. Prepare Payload
		Map<String, Object> payload = new HashMap<>();
		payload.put("branch_id", request.branchId());
		payload.put("sync_mode", "FULL_PULL");

		// 3. Create Log (Entity ID is the Branch ID)
		// Note: We no longer save 'savedLog' to pass into the queue
		posLogRepository.save(PosLog.builder()
				.branchId(request.branchId())
				.entityId(request.branchId()) // Branch is the entity for menu syncs
				.entityType("MENU_CATALOG")
				.action("SYNC_MENU")
				.requestPayload(payload)
				.status("PENDING")
				.attemptCount(0)
				.build());

		// 4. Create Queue Entry (DECOUPLED)
		// We pass branchId and action so the worker knows what to do
		posSyncQueueRepository.save(PosSyncQueue.builder()
				.branchId(request.branchId())
				.action("SYNC_MENU")
				.payload(payload)
				.status("PENDING")
				.nextRetryAt(OffsetDateTime.now())
				.retryCount(0)
				.build());
	}


	@Override
	@Transactional
	public void forceSyncByBranchId(UUID branchId) {
		log.info("Starting bulk force-sync for branch: {}", branchId);

		// 1. Find all FAILED logs for this specific branch
		// Note: Using the branch_id from your updated sync_logs or metadata in pos_logs
		List<PosLog> failedLogs = posLogRepository.findAllFailedByBranch(branchId.toString());

		for (PosLog logEntry : failedLogs) {
			logEntry.setStatus("PENDING");
			posLogRepository.save(logEntry);

			// Logic changed: find by branch and entity instead of logId
			PosSyncQueue queueItem = posSyncQueueRepository
					.findByBranchIdAndBookingId(branchId, logEntry.getEntityId())
					.orElse(new PosSyncQueue());

			queueItem.setBranchId(branchId);
			queueItem.setBookingId(logEntry.getEntityId());
			queueItem.setAction(logEntry.getEntityType().equals("ORDER") ? "SEND_ORDER" : "RESERVE_TABLE");
			queueItem.setStatus("PENDING");
			queueItem.setNextRetryAt(OffsetDateTime.now());
			queueItem.setRetryCount(0);

			posSyncQueueRepository.save(queueItem);
		}
	}

	@Transactional
	public void saveLog(UUID branchId, UUID entityId, String entityType, String action, Object request, String response, String status) {
		// 1. Convert payload to Map for the JSONB column
		Map<String, Object> payload = objectMapper.convertValue(request, new TypeReference<>() {
		});

		// 2. Build and Save Log
		PosLog logEntry = PosLog.builder()
				.branchId(branchId)
				.entityId(entityId)
				.entityType(entityType)
				.action(action)
				.requestPayload(payload)
				.responsePayload(response)
				.status(status)
				.attemptCount(1) // Or increment if you track retries
				.build();

		posLogRepository.save(logEntry);
	}
}
