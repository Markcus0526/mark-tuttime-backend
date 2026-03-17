package com.tuttime.integrationservice.controller;

import java.util.List;
import java.util.Map;

import com.tuttime.integrationservice.entity.IntegrationCredentials;
import com.tuttime.integrationservice.repository.IntegrationCredentialsRepository;
import com.tuttime.integrationservice.repository.SyncLogRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
public class IntegrationController {
  private final IntegrationCredentialsRepository credentialsRepo;
  private final SyncLogRepository syncLogRepo;

  @PostMapping("/credentials")
  public IntegrationCredentials saveCredentials(@RequestBody Map<String, Object> req) {
    IntegrationCredentials cred = new IntegrationCredentials();
    cred.setRestaurantId((Long) req.get("restaurantId"));
    cred.setPosSystem(IntegrationCredentials.PosSystem.valueOf((String) req.get("posSystem")));
    cred.setApiUrl((String) req.get("apiUrl"));
    cred.setApiKey((String) req.get("apiKey"));
    cred.setApiToken((String) req.get("apiToken"));
    return credentialsRepo.save(cred);
  }

  @GetMapping("/logs/{restaurantId}")
  public List logs(@PathVariable Long restaurantId) {
    return syncLogRepo.findByRestaurantIdAndStatusOrderBySyncTimeDesc(restaurantId, "ERROR");
  }

  @PostMapping("/force-sync/{restaurantId}")
  public String forceSync(@PathVariable Long restaurantId) {
    // Trigger sync
    return "Sync triggered";
  }
}
