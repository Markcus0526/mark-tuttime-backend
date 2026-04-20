package com.tuttime.pos.service;

import com.tuttime.pos.dto.*;
import com.tuttime.pos.entity.PosSystemConfig;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

public interface PosService {

	// Manual/On-demand Sync
	void syncBooking(BookingSyncRequest request);
	void syncOrder(OrderSyncRequest request);
	void syncMenu(MenuSyncRequest request);

	// Maintenance & Recovery
	void forceSyncByBranchId(UUID branchId);
	PosSyncLogResponse getSyncLogs(UUID entityId);

	// Configuration
	PosSystemConfig saveConfig(PosCredentialsDto dto);

	void markSuccess(int logId, Map<String, String> metadata);

	void markFailed(int logId, String errorMessage, Map<String, String> metadata);

	void markAsHardFailure(UUID orderId);

}
