package com.tuttime.pos.scheduler;

import com.tuttime.pos.service.PosSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosSyncRetryScheduler {

	private final PosSyncService posSyncService;

	// Run every 1 minute
	@Scheduled(fixedDelay = 60000)
	public void retryFailedSyncs() {
		posSyncService.processRetryQueue();
	}
}
