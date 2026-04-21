package com.tuttime.pos.scheduler;

import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.adapter.PosAdapterFactory;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PosRetryScheduler {

	private final PosSyncQueueRepository queueRepository;
	private final PosConfigRepository configRepository;
	private final PosAdapterFactory adapterFactory;

	@Scheduled(fixedDelay = 60000) // Runs every 60 seconds
	@Transactional
	public void processQueue() {
		List<PosSyncQueue> pendingTasks = queueRepository
				.findAllByStatusAndNextRetryAtBefore("PENDING", OffsetDateTime.now());

		for (PosSyncQueue task : pendingTasks) {
			try {
				attemptSync(task);
				task.setStatus("COMPLETED");
				log.info("Retry successful for Task ID: {}", task.getId());
			} catch (Exception e) {
				handleFailedRetry(task, e);
			}
			queueRepository.save(task);
		}
	}

	private void attemptSync(PosSyncQueue task) {
		PosConfig config = configRepository.findByBranchIdAndEnabledTrue(task.getBranchId())
				.orElseThrow(() -> new RuntimeException("Config missing or disabled"));

		PosAdapter adapter = adapterFactory.getAdapter(config.getPosProvider());

		// Use the action string to call the correct adapter method
		switch (task.getAction()) {
			case "RESERVE_TABLE" -> adapter.reserveTable(config, null, task.getBookingId());
			case "OCCUPY_TABLE"  -> adapter.occupyTable(config, null, task.getBookingId());
			case "RELEASE_TABLE" -> adapter.releaseTable(config, null, task.getBookingId());
			default -> log.error("Unknown action in queue: {}", task.getAction());
		}
	}

	private void handleFailedRetry(PosSyncQueue task, Exception e) {
		int newRetryCount = task.getRetryCount() + 1;

		if (newRetryCount >= 5) {
			task.setStatus("FAILED_PERMANENTLY");
			log.error("Task {} failed permanently after 5 attempts: {}", task.getId(), e.getMessage());
		} else {
			// Exponential backoff: 2, 4, 8, 16 minutes
			long waitMinutes = (long) Math.pow(2, newRetryCount);
			task.setRetryCount(newRetryCount);
			task.setNextRetryAt(OffsetDateTime.now().plusMinutes(waitMinutes));
			task.setLastError(e.getMessage());
			log.warn("Retry {} failed for task {}. Next try in {} minutes.", newRetryCount, task.getId(), waitMinutes);
		}
	}
}
