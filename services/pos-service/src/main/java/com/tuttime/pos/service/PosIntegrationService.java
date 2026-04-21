package com.tuttime.pos.service;

import org.springframework.http.ResponseEntity;

public interface PosIntegrationService {

	ResponseEntity<String> processWebhook(String posSystem, String payload, String signature);
}
