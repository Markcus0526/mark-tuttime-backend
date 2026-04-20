package com.tuttime.pos.service;

import com.tuttime.pos.dto.*;
import com.tuttime.pos.entity.PosSystemConfig;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

public interface PosSyncService {

	ResponseEntity<String> processWebhook(String posSystem, String payload, String signature);
}
