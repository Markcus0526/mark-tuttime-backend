package com.tuttime.pos.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.pos.dto.*;
import com.tuttime.pos.entity.PosLog;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.SyncLog;
import com.tuttime.pos.mapper.PosMapper;
import com.tuttime.pos.repository.PosLogRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.repository.SyncLogRepository;
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
	private final SyncLogRepository syncLogRepository;
	private final PosSyncQueueRepository posSyncQueueRepository;
	private final PosMapper posMapper;

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

		// 2. Prepare Payload (Critical for your JSONB indexing)
		Map<String, Object> payload = new HashMap<>();
		payload.put("order_id", request.orderId());
		payload.put("branch_id", request.branchId()); // Must be included!
		payload.put("sync_type", "MANUAL");

		// 3. Create Audit Log Entry
		PosLog logEntry = PosLog.builder()
				.config(config)
				.entityType("ORDER")
				.entityId(request.orderId())
				.status("PENDING")
				.attemptCount(0) // 0 because no API call has been made yet
				.requestPayload(payload) // Matches your JSONB column
				.build();

		PosLog savedLog = posLogRepository.save(logEntry);

		// 3. Create Queue Entry for the background worker/retry logic
		PosSyncQueue queueEntry = PosSyncQueue.builder()
				.posLog(savedLog)
				.status("PENDING")
				.nextRetryAt(OffsetDateTime.now()) // Try immediately
				.retryCount(0)
				.build();

		posSyncQueueRepository.save(queueEntry);
	}

	// Stubbed methods for Booking and Menu (similar patterns to syncOrder)
	@Override
	@Transactional
	public void syncBooking(PosSyncDto.BookingSyncRequest request) {
		log.info("Queuing booking sync for ID: {} at branch: {}", request.bookingId(), request.branchId());

		PosConfig config = posConfigRepository.findByBranchId(request.branchId())
				.orElseThrow(() -> new InvalidRequestException("POS Config not found for " + request.branchId()));

		// 1. Construct Payload for the background worker
		Map<String, Object> payload = new HashMap<>();
		payload.put("booking_id", request.bookingId());
		payload.put("branch_id", request.branchId());
		payload.put("booking_time", request.bookingTime());
		payload.put("sync_type", "MANUAL");

		// 2. Create POS Log
		PosLog logEntry = PosLog.builder()
				.config(config)
				.entityType("BOOKING")
				.entityId(request.bookingId())
				.status("PENDING")
				.requestPayload(payload) // Critical for JSONB indexing
				.attemptCount(0)
				.build();
		PosLog savedLog = posLogRepository.save(logEntry);

		// 3. Queue for the Scheduler
		PosSyncQueue queueEntry = PosSyncQueue.builder()
				.posLog(savedLog)
				.status("PENDING")
				.nextRetryAt(OffsetDateTime.now())
				.retryCount(0)
				.build();
		posSyncQueueRepository.save(queueEntry);
	}

	@Override
	@Transactional
	public void syncMenu(PosSyncDto.MenuSyncRequest request) {
		log.info("Initializing menu catalog pull for branch: {}", request.branchId());

		PosConfig config = posConfigRepository.findByBranchId(request.branchId())
				.orElseThrow(() -> new InvalidRequestException("POS Config not found for " + request.branchId()));

		Map<String, Object> payload = new HashMap<>();
		payload.put("branch_id", request.branchId());
		payload.put("sync_mode", "FULL_PULL"); // To distinguish between incremental and full

		// 1. Create Log (Entity ID is the Branch ID here)
		PosLog logEntry = PosLog.builder()
				.config(config)
				.entityType("MENU_CATALOG")
				.entityId(request.branchId())
				.status("PENDING")
				.requestPayload(payload)
				.attemptCount(0)
				.build();
		PosLog savedLog = posLogRepository.save(logEntry);

		// 2. Queue for Scheduler
		// Note: Menu sync might need a separate priority queue or longer retry backoff
		PosSyncQueue queueEntry = PosSyncQueue.builder()
				.posLog(savedLog)
				.status("PENDING")
				.nextRetryAt(OffsetDateTime.now())
				.retryCount(0)
				.build();
		posSyncQueueRepository.save(queueEntry);
	}

	@Override
	@Transactional
	public void forceSyncByBranchId(UUID branchId) {
		log.info("Starting bulk force-sync for branch: {}", branchId);

		// 1. Find all FAILED logs for this specific branch
		// Note: Using the branch_id from your updated sync_logs or metadata in pos_logs
		List<PosLog> failedLogs = posLogRepository.findAllFailedByBranch(branchId.toString());

		if (failedLogs.isEmpty()) {
			log.info("No failed tasks found for branch: {}", branchId);
			return;
		}

		for (PosLog logEntry : failedLogs) {
			// 2. Reset the Log Status
			logEntry.setStatus("PENDING");
			logEntry.setAttemptCount(0); // Reset attempts to give it a fresh start
			posLogRepository.save(logEntry);

			// 3. Upsert into the Sync Queue for immediate processing
			PosSyncQueue queueItem = posSyncQueueRepository.findByPosLogId(logEntry.getId())
					.orElse(new PosSyncQueue());

			queueItem.setPosLog(logEntry);
			queueItem.setNextRetryAt(OffsetDateTime.now()); // Set to "Now" to trigger immediate retry
			queueItem.setRetryCount(0);
			queueItem.setStatus("PENDING");

			posSyncQueueRepository.save(queueItem);
		}

		log.info("Successfully re-queued {} tasks for branch {}", failedLogs.size(), branchId);
	}

	@Transactional
	public void markSuccess(int logId, Map<String, String> metadata) {
		// 1. Fetch the existing log record
		SyncLog log = syncLogRepository.findById(logId)
				.orElseThrow(() -> new InvalidRequestException("Log not found for ID: " + logId));

		// 2. Update fields
		log.setStatus("SUCCESS");
		log.setMetadata(metadata); // Assuming this is a JSONB or Map column
		log.setUpdatedAt(OffsetDateTime.now()); // Your preferred Java type!

		// 3. Save changes
		syncLogRepository.save(log);
	}

	@Transactional
	public void markFailed(int logId, String errorMessage, Map<String, String> additionalDetails) {
		// 1. Fetch the existing log record
		SyncLog log = syncLogRepository.findById(logId)
				.orElseThrow(() -> new InvalidRequestException("Log not found for ID: " + logId));

		// 2. Set status to FAILED (or SOFT_FAILURE)
		log.setStatus("FAILED");

		// 3. Build and save error metadata
		Map<String, String> metadata = new HashMap<>(additionalDetails != null ? additionalDetails : Map.of());
		metadata.put("error_message", errorMessage);
		log.setMetadata(metadata);

		// updatedAt is handled automatically by BaseEntity @LastModifiedDate
		syncLogRepository.save(log);
	}

	@Transactional
	public void markAsHardFailure(UUID orderId) {
		// 1. Find the log associated with this order
		// Note: You might need a custom finder if orderId is not the Primary Key
		SyncLog log = syncLogRepository.findByOrderId(orderId)
				.orElseThrow(() -> new InvalidRequestException("Sync log not found for order: " + orderId));

		// 2. Update to a terminal failure state
		log.setStatus("HARD_FAILURE");

		// 3. Optional: Clear or update metadata with failure details
		log.setMetadata(Map.of("error_type", "TERMINAL", "reason", "Manual hard failure trigger"));

		// Save is handled by @Transactional + Auditing for updatedAt
		syncLogRepository.save(log);
	}


}
