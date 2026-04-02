package com.tuttime.pos.scheduler;

import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import com.tuttime.pos.service.PosIntegrationService;
import com.tuttime.pos.service.PosSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PosSyncScheduler {

	private final PosSyncQueueRepository queueRepository;
	private final PosSyncService posSyncService;
	private final PosIntegrationService posIntegrationService;

	/**
	 * Runs every 2 minutes to process pending or failed sync tasks.
	 * fixedDelay ensures one task finishes before the next one starts.
	 */
	@Scheduled(fixedDelay = 120000)
	public void processSyncQueue() {
		log.info("Starting POS background sync process at {}", OffsetDateTime.now());

		// 1. Find all tasks where next_retry_at is in the past
		List<PosSyncQueue> pendingTasks = queueRepository
				.findAllByStatusAndNextRetryAtBefore("PENDING", OffsetDateTime.now());

		if (pendingTasks.isEmpty()) {
			return;
		}

		log.info("Found {} tasks to synchronize.", pendingTasks.size());

		for (PosSyncQueue task : pendingTasks) {
			try {
				// 2. Mark as PROCESSING in DB to prevent other instances from picking it up
				task.setStatus("PROCESSING");
				queueRepository.save(task);

				// 3. Attempt physical sync to iiko/R-Keeper
				// This uses the Logic/gRPC we built earlier
				posIntegrationService.pushOrderToRestaurant(task.getLog().getEntityId());

				// 4. On Success: Log success and remove from queue
				posSyncService.markSuccess(task.getLog().getId(), Map.of("status", "SUCCESS"));

			} catch (Exception e) {
				// 5. On Failure: Update retry count and schedule next attempt
				handleSyncFailure(task, e.getMessage());
			}
		}
	}

	private void handleSyncFailure(PosSyncQueue task, String error) {
		int newRetryCount = task.getRetryCount() + 1;

		if (newRetryCount >= 5) { // Hard limit of 5 retries
			log.error("Sync failed for Order {} after maximum retries.", task.getLog().getEntityId());
			task.setStatus("FAILED");
			posSyncService.markFailed(task.getLog().getId(), "MAX_RETRIES_REACHED", Map.of("status", error));
		} else {
			task.setStatus("PENDING");
			task.setRetryCount(newRetryCount);
			// Exponential backoff: 5m, 10m, 20m, 40m...
			task.setNextRetryAt(OffsetDateTime.now().plusMinutes((long) Math.pow(2, newRetryCount) * 5));
		}

		queueRepository.save(task);
	}
}
