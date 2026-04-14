package com.tuttime.pos.service.impl;

import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.common.exception.ResourceNotFoundException;
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
import jakarta.persistence.EntityNotFoundException;
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
@Transactional(readOnly = true)
@Slf4j
public class PosSyncServiceImpl implements PosSyncService {

	private final PosLogRepository logRepository;
	private final SyncLogRepository syncLogRepository;
	private final PosSystemConfigRepository configRepository;
	private final PosSyncQueueRepository queueRepository;
	private final PosMapper posMapper;

	@Override
	public PosSyncLogResponse getSyncLogs(UUID entityId) {
		// Fetch all logs for this entity (Order or Booking)
		List<PosLog> logs = logRepository.findAllByEntityIdOrderByCreatedAtDesc(entityId);
		if (logs.isEmpty()) {
			throw new MethodArgumentNotValidException("No sync history found for entity: " + entityId);
		}
		return posMapper.toResponse(entityId, logs);
	}

	@Override
	@Transactional
	public PosSystemConfig saveConfig(PosCredentialsDto dto) {
		// Check if config exists or create new
		PosSystemConfig config = configRepository.findByPosSystem(dto.posSystem())
				.orElse(new PosSystemConfig());

		config.setPosSystem(dto.posSystem());
		config.setApiEndpoint(dto.apiUrl());
		config.setWebhookSecret(dto.apiKey()); // Mapping example
		config.setEnabled(true);

		return configRepository.save(config);
	}

	@Override
	@Transactional
	public void syncOrder(OrderSyncRequest request) {
		log.info("Manually triggering sync for order: {}", request.orderId());

		// 1. Get POS Config based on the request (dynamic lookup)
		PosSystemConfig config = configRepository.findByPosSystem(request.posSystem())
				.orElseThrow(() -> new MethodArgumentNotValidException("POS System not configured: " + request.posSystem()));

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
	public void forceSyncByRestaurantId(UUID restaurantId) {
		// Logic to find all FAILED tasks in queue and reset them to PENDING
		List<PosSyncQueue> failedTasks = queueRepository.findAllFailedByRestaurantId(restaurantId);
		failedTasks.forEach(task -> {
			task.setStatus("PENDING");
			task.setNextRetryAt(OffsetDateTime.now());
			task.setRetryCount(0);
		});
		queueRepository.saveAll(failedTasks);
		log.info("Reset {} failed tasks for restaurant {}", failedTasks.size(), restaurantId);
	}

	@Transactional
	public void markSuccess(int logId, Map<String, String> metadata) {
		// 1. Fetch the existing log record
		SyncLog log = syncLogRepository.findById(logId)
				.orElseThrow(() -> new MethodArgumentNotValidException("Log not found for ID: " + logId));

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
				.orElseThrow(() -> new MethodArgumentNotValidException("Log not found for ID: " + logId));

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
				.orElseThrow(() -> new MethodArgumentNotValidException("Sync log not found for order: " + orderId));

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
