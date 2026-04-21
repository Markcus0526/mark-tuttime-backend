package com.tuttime.pos.service;

import org.springframework.http.ResponseEntity;

public interface PosSyncService {

	ResponseEntity<String> processWebhook(String posSystem, String payload, String signature);
}
