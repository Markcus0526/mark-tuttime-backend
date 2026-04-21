package com.tuttime.pos.scheduler;

import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.adapter.PosAdapterFactory;
import com.tuttime.pos.adapter.PosAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PosMenuScheduler {

	private final PosConfigRepository posConfigRepository;
	private final PosAdapterFactory adapterFactory;

	/**
	 * Runs daily at 2:00 AM to sync nomenclature for all active branches.
	 * Cron format: [second] [minute] [hour] [day of month] [month] [day of week]
	 */
	@Scheduled(cron = "${app.pos.menu-sync-cron:0 0 2 * * *}")
	public void scheduleDailyMenuSync() {
		log.info("Starting automated daily menu synchronization...");

		// 1. Fetch all active POS configurations from your pos_configs table
		List<PosConfig> activeConfigs = posConfigRepository.findAllByEnabledTrue();

		for (PosConfig config : activeConfigs) {
			try {
				log.info("Syncing menu for branch: {} (Provider: {})",
						config.getBranchId(), config.getPosProvider());

				// 2. Get the correct adapter (iiko/R-Keeper)
				PosAdapter adapter = adapterFactory.getAdapter(config.getPosProvider());

				// 3. Execute the nomenclature pull
				adapter.syncMenu(config);

				log.info("Successfully synced branch: {}", config.getBranchId());
			} catch (Exception e) {
				// Log failure but continue to the next branch
				log.error("Failed to sync menu for branch {}: {}",
						config.getBranchId(), e.getMessage());
			}
		}
		log.info("Automated daily menu sync completed.");
	}
}
