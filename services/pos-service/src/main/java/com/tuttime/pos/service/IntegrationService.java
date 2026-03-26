package com.tuttime.pos.service;

import com.tuttime.pos.entity.PosCredentials;
import com.tuttime.pos.repository.PosCredentialsRepository;
import com.tuttime.pos.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {
	private final PosCredentialsRepository credentialsRepo;
	private final SyncLogRepository syncLogRepo;
	private final IikoConnector iikoConnector;
	private final RKeeperConnector rKeeperConnector;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final WebClient.Builder webClientBuilder;

	@Scheduled(fixedRate = 30000) // 30s poll
	public void pollMenus() {
		List<PosCredentials> creds = credentialsRepo.findAll();
		creds.forEach(cred -> {
			switch (cred.getPosSystem()) {
				case IIKO:
					iikoConnector.getMenu(cred.getRestaurantId(), cred.getApiUrl(), cred.getApiToken())
							.publishOn(Schedulers.boundedElastic())
							.subscribe();
					break;
				case RKEEPER:
					rKeeperConnector.getMenu(cred.getRestaurantId(), cred.getApiUrl(), cred.getApiKey())
							.publishOn(Schedulers.boundedElastic())
							.subscribe();
					break;
			}
		});
	}

	public void sendOrder(Long restaurantId, Map<String, Object> order) {
		credentialsRepo.findByRestaurantIdAndPosSystem(restaurantId, PosCredentials.PosSystem.IIKO)
				.ifPresent(cred -> iikoConnector.sendOrder(restaurantId, cred.getApiUrl(), cred.getApiToken(), order)
						.subscribe());
		// RKeeper similar
	}

	// DLQ for failed syncs - Kafka topic "dead-letter"
	// Extend for table occupancy (optional)
}
