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
import com.tuttime.pos.service.PosService;
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
public class PosServiceImpl implements PosService {

	private final PosSystemConfigRepository posSystemConfigRepository;
	private final PosLogRepository logRepository;
	private final SyncLogRepository syncLogRepository;
	private final PosSystemConfigRepository configRepository;
	private final PosSyncQueueRepository queueRepository;
	private final PosMapper posMapper;

	@Override
	@Transactional(readOnly = true)
	public PosSyncLogResponse getSyncLogs(UUID entityId) {
		// Fetch all logs for this entity (Order or Booking)
		List<PosLog> logs = logRepository.findAllByEntityIdOrderByCreatedAtDesc(entityId);
		if (logs.isEmpty()) {
			throw new InvalidRequestException("No sync history found for entity: " + entityId);
		}
		return posMapper.toResponse(entityId, logs);
	}

	@Override
	@Transactional
	public PosSystemConfig saveConfig(PosCredentialsDto dto) {
		// Implementation for your pos_system_configs table
		PosSystemConfig config = configRepository.findByPosProvider(dto.provider())
				.orElse(new PosSystemConfig());

		config.setPosProvider(dto.provider());
		config.setApiEndpoint(dto.apiEndpoint());
		config.setWebhookSecret(dto.webhookSecret()); // Encrypt this in a real app!
		config.setEnabled(true);

		return configRepository.save(config);
	}

	@Override
	@Transactional
	public void syncOrder(OrderSyncRequest request) {
		log.info("Manually triggering sync for order: {}", request.orderId());

		// 1. Get POS Config based on the request (dynamic lookup)
		PosSystemConfig config = configRepository.findByPosSystem(request.posSystem())
				.orElseThrow(() -> new InvalidRequestException("POS System not configured: " + request.posSystem()));

		// 2. Create Audit Log Entry
		PosLog logEntry = PosLog.builder()
				.config(config)
				.entityType("ORDER")
				.entityId(request.orderId())
				.status("PENDING")
				.attemptCount(1)
				.lastAttempt(OffsetDateTime.now())
				.build();

		PosLog savedLog = logRepository.save(logEntry);

		// 3. Create Queue Entry for the background worker/retry logic
		PosSyncQueue queueEntry = PosSyncQueue.builder()
				.log(savedLog)
				.status("PENDING")
				.nextRetryAt(OffsetDateTime.now()) // Try immediately
				.retryCount(0)
				.build();

		queueRepository.save(queueEntry);
	}

	@Override
	@Transactional
	public void forceSyncByRestaurantId(UUID branchId) {
		log.info("Starting bulk force-sync for branch: {}", branchId);

		// 1. Find all FAILED logs for this specific branch
		// Note: Using the branch_id from your updated sync_logs or metadata in pos_logs
		List<PosLog> failedLogs = posLogRepository.findAllFailedByBranch(branchId);

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
			PosSyncQueue queueItem = syncQueueRepository.findByPosLogId(logEntry.getId())
					.orElse(new PosSyncQueue());

			queueItem.setPosLog(logEntry);
			queueItem.setNextRetryAt(OffsetDateTime.now()); // Set to "Now" to trigger immediate retry
			queueItem.setRetryCount(0);
			queueItem.setStatus("PENDING");

			syncQueueRepository.save(queueItem);
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

	// Stubbed methods for Booking and Menu (similar patterns to syncOrder)
	@Override
	public void syncBooking(BookingSyncRequest request) { /* Implementation */ }

	@Override
	public void syncMenu(MenuSyncRequest request) { /* Implementation */ }

}
