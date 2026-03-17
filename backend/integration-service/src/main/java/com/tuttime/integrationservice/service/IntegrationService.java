package com.tuttime.integrationservice.service;

import com.tuttime.integrationservice.entity.IntegrationCredentials;
import com.tuttime.integrationservice.entity.SyncLog;
import com.tuttime.integrationservice.repository.IntegrationCredentialsRepository;
import com.tuttime.integrationservice.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {
  private final IntegrationCredentialsRepository credentialsRepo;
  private final SyncLogRepository syncLogRepo;
  private final IikoConnector iikoConnector;
  private final RKeeperConnector rKeeperConnector;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final WebClient.Builder webClientBuilder;

  @Scheduled(fixedRate = 30000) // 30s poll
  public void pollMenus() {
    List<IntegrationCredentials> creds = credentialsRepo.findAll();
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

  public void sendOrder(Long restaurantId, Object order) {
    credentialsRepo.findByRestaurantIdAndPosSystem(restaurantId, IntegrationCredentials.PosSystem.IIKO)
      .ifPresent(cred -> iikoConnector.sendOrder(restaurantId, cred.getApiUrl(), cred.getApiToken(), order)
        .subscribe());
    // RKeeper similar
  }

  // DLQ for failed syncs - Kafka topic "dead-letter"
  // Extend for table occupancy (optional)
}
