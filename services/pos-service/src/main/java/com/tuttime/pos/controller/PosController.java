package com.tuttime.pos.controller;

import com.tuttime.pos.dto.*;
import com.tuttime.pos.entity.PosCredentials;
import com.tuttime.pos.repository.PosCredentialsRepository;
import com.tuttime.pos.repository.SyncLogRepository;
import com.tuttime.pos.service.PosSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
public class PosController {
	private final PosCredentialsRepository posCredentialsRepository;
	private final SyncLogRepository syncLogRepo;
	private final PosSyncService posSyncService;

	@PostMapping("/sync/booking")
	public ResponseEntity<Void> syncBooking(@RequestBody BookingSyncRequest request) {
		posSyncService.syncBooking(request);
		return ResponseEntity.accepted().build();
	}

	@PostMapping("/sync/order")
	public ResponseEntity<Void> syncOrder(@RequestBody OrderSyncRequest request) {
		posSyncService.syncOrder(request);
		return ResponseEntity.accepted().build();
	}

	@PostMapping("/sync/menu")
	public ResponseEntity<Void> syncMenu(@RequestBody MenuSyncRequest request) {
		posSyncService.syncMenu(request);
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/logs/entity/{entityId}")
	public ResponseEntity<PosSyncLogResponse> getLogsByEntityId(@PathVariable String entityId) {
		PosSyncLogResponse response = posSyncService.getSyncLogs(entityId);
		return ResponseEntity.ok(response);
	}

//	@GetMapping("/logs/restaurant/{restaurantId}")
//	public ResponseEntity<List<PosSyncLogDto>> getErrorLogsByRestaurantId(@PathVariable Long restaurantId) {
//		List<PosSyncLogDto> logs = syncLogRepo.findByRestaurantIdAndStatusOrderBySyncTimeDesc(restaurantId, "ERROR");
//		return ResponseEntity.ok(logs);
//	}

	@PostMapping("/credentials")
	public ResponseEntity<PosCredentials> saveCredentials(@Valid @RequestBody PosCredentialsDto credentialsDto) {
		PosCredentials.PosSystem posSystem;
		try {
			posSystem = PosCredentials.PosSystem.valueOf(credentialsDto.getPosSystem());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build(); // or return a proper error response
		}

		PosCredentials cred = new PosCredentials();
		cred.setRestaurantId(credentialsDto.getRestaurantId());
		cred.setPosSystem(posSystem);
		cred.setApiUrl(credentialsDto.getApiUrl());
		cred.setApiKey(credentialsDto.getApiKey());
		cred.setApiToken(credentialsDto.getApiToken());

		PosCredentials saved = posCredentialsRepository.save(cred);
		return ResponseEntity.ok(saved);
	}

	@PostMapping("/force-sync/{restaurantId}")
	public ResponseEntity<String> forceSync(@PathVariable Long restaurantId) {
		// Example: trigger sync logic for restaurant via the service
//		posSyncService.forceSyncByRestaurantId(restaurantId);
		return ResponseEntity.accepted().body("Sync triggered for restaurantId: " + restaurantId);
	}
}
