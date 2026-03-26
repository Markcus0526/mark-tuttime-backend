package com.tuttime.pos.service.impl;

import com.tuttime.pos.adapter.PosAdapterFactory;
import com.tuttime.pos.dto.*;
import com.tuttime.pos.entity.PosLog;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.entity.PosSystemConfig;
import com.tuttime.pos.kafka.PosEventProducer;
import com.tuttime.pos.repository.PosLogRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import com.tuttime.pos.repository.PosSystemConfigRepository;
import com.tuttime.pos.service.PosSyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PosSyncServiceImpl implements PosSyncService {

	private static final Logger logger = LoggerFactory.getLogger(PosSyncServiceImpl.class);

	private final PosLogRepository posLogRepository;
	private final PosSyncQueueRepository posSyncQueueRepository;
	private final PosSystemConfigRepository posSystemConfigRepository;
	private final PosAdapterFactory posAdapterFactory;
	private final PosEventProducer posEventProducer;

	@Override
	@Transactional
	public void syncBooking(BookingSyncRequest request) {
		PosLog posLog = mapBookingRequestToPosLog(request);
		syncToPos(posLog);
	}

	@Override
	@Transactional
	public void syncOrder(OrderSyncRequest request) {
		PosLog posLog = mapOrderRequestToPosLog(request);
		syncToPos(posLog);
	}

	@Override
	@Transactional
	public void syncMenu(MenuSyncRequest request) {
		PosLog posLog = mapMenuRequestToPosLog(request);
		syncToPos(posLog);
	}

	@Retryable(value = {RuntimeException.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
	@Transactional
	public void syncToPos(PosLog posLog) {
		logger.info("Starting POS sync attempt for entity: {} id: {}", posLog.getEntityType(), posLog.getEntityId());

		boolean success = posAdapterFactory.getAdapter(posLog.getPosSystemConfig().getPosSystem())
				.syncEntity(posLog.getEntityType(), posLog.getEntityId());

		posLog.setAttemptCount(posLog.getAttemptCount() + 1);
		posLog.setLastAttempt(Instant.now());

		if (!success) {
			posLog.setStatus("FAILED");
			posLogRepository.save(posLog);
			posEventProducer.publishSyncResult(
					posLog.getEntityId(),
					posLog.getEntityType(),
					posLog.getPosSystemConfig().getPosSystem(),
					"FAILED",
					"POS sync failed"
			);
			throw new RuntimeException("POS sync failed for log id: " + posLog.getId());
		}

		posLog.setStatus("SUCCESS");
		posLogRepository.save(posLog);

		posSyncQueueRepository.findByPosLog(posLog).ifPresent(posSyncQueueRepository::delete);

		posEventProducer.publishSyncResult(
				posLog.getEntityId(),
				posLog.getEntityType(),
				posLog.getPosSystemConfig().getPosSystem(),
				"SUCCESS",
				"POS sync succeeded"
		);

		logger.info("POS sync succeeded for entity: {} id: {}", posLog.getEntityType(), posLog.getEntityId());
	}

	@Recover
	@Transactional
	public void recover(RuntimeException e, PosLog posLog) {
		logger.error("POS sync failed after retries for log id: {}. Scheduling retry.", posLog.getId(), e);

		posLog.setStatus("FAILED");
		posLogRepository.save(posLog);

		PosSyncQueue queueEntry = posSyncQueueRepository.findByPosLog(posLog)
				.orElse(PosSyncQueue.builder()
						.posLog(posLog)
						.retryCount(0)
						.status("PENDING")
						.nextRetryAt(Instant.now().plus(2, ChronoUnit.MINUTES))
						.build());

		queueEntry.setRetryCount(queueEntry.getRetryCount() + 1);
		queueEntry.setNextRetryAt(Instant.now().plus((long) Math.pow(2, queueEntry.getRetryCount()), ChronoUnit.MINUTES));
		queueEntry.setStatus("PENDING");
		posSyncQueueRepository.save(queueEntry);
	}

	@Override
	@Transactional
	public void processRetryQueue() {
		List<PosSyncQueue> dueRetries = posSyncQueueRepository.findByStatusAndNextRetryAtBefore("PENDING", Instant.now());
		for (PosSyncQueue queue : dueRetries) {
			try {
				syncToPos(queue.getPosLog());
				queue.setStatus("COMPLETED");
				posSyncQueueRepository.save(queue);
			} catch (Exception e) {
				logger.error("Retry attempt failed for posLogId: {}", queue.getPosLog().getId(), e);
				queue.setRetryCount(queue.getRetryCount() + 1);
				queue.setNextRetryAt(Instant.now().plus((long) Math.pow(2, queue.getRetryCount()), ChronoUnit.MINUTES));
				if (queue.getRetryCount() > 5) {
					queue.setStatus("FAILED");
				}
				posSyncQueueRepository.save(queue);
			}
		}
	}

	@Override
	public PosSyncLogResponse getSyncLogs(String entityId) {
		List<PosLog> logs = posLogRepository.findByEntityIdOrderByLastAttemptDesc(entityId);

		// Convert List<PosLog> to List<PosSyncLogEntry>
		List<PosSyncLogEntry> logEntries = logs.stream()
				.map(this::convertToPosSyncLogEntry)
				.toList();  // toList() requires Java 16+, else use collect(Collectors.toList())

		return new PosSyncLogResponse(entityId, logEntries);
	}

	// Mapping helper methods

	private PosLog mapBookingRequestToPosLog(BookingSyncRequest request) {
		PosSystemConfig posSystemConfig = posSystemConfigRepository
				.findByPosSystem(request.getPosSystem())
				.orElseThrow(() -> new IllegalArgumentException("Unsupported POS system: " + request.getPosSystem()));

		return PosLog.builder()
				.entityType("BOOKING")
				.entityId(request.getBookingId())
				.posSystemConfig(posSystemConfig)
				.status("PENDING")
				.attemptCount(0)
				.build();
	}

	private PosLog mapOrderRequestToPosLog(OrderSyncRequest request) {
		PosSystemConfig posSystemConfig = posSystemConfigRepository
				.findByPosSystem(request.getPosSystem())
				.orElseThrow(() -> new IllegalArgumentException("Unsupported POS system: " + request.getPosSystem()));

		return PosLog.builder()
				.entityType("ORDER")
				.entityId(request.getOrderId())
				.posSystemConfig(posSystemConfig)
				.status("PENDING")
				.attemptCount(0)
				.build();
	}

	private PosLog mapMenuRequestToPosLog(MenuSyncRequest request) {
		PosSystemConfig posSystemConfig = posSystemConfigRepository
				.findByPosSystem(request.getPosSystem())
				.orElseThrow(() -> new IllegalArgumentException("Unsupported POS system: " + request.getPosSystem()));

		return PosLog.builder()
				.entityType("MENU")
				.entityId(request.getRestaurantId())
				.posSystemConfig(posSystemConfig)
				.status("PENDING")
				.attemptCount(0)
				.build();
	}

	private PosSyncLogEntry convertToPosSyncLogEntry(PosLog log) {
		// Map fields from PosLog entity to PosSyncLogEntry DTO
		// TODO:
		return PosSyncLogEntry.builder()
//				.id(log.getId())
//				.entityType(log.getEntityType())
//				.entityId(log.getEntityId())
				.status(log.getStatus())
//				.attemptCount(log.getAttemptCount())
//				.lastAttempt(log.getLastAttempt())
				// map other relevant fields as needed
				.build();
	}
}