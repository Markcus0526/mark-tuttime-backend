package com.tuttime.pos.service;

import com.tuttime.pos.dto.*;
import com.tuttime.pos.entity.PosConfig;

import java.util.Map;
import java.util.UUID;

public interface PosService {

	// Manual/On-demand Sync
	void syncBooking(PosSyncDto.BookingSyncRequest request);
	void syncOrder(PosSyncDto.OrderSyncRequest request);
	void syncMenu(PosSyncDto.MenuSyncRequest request);

	// Maintenance & Recovery
	void forceSyncByBranchId(UUID branchId);
	PosSyncLogDto.PosSyncLogResponse getSyncLogs(UUID entityId);

	// Configuration
	PosConfig saveConfig(PosConfigDto.PosConfigRequest request);

	void markSuccess(int logId, Map<String, String> metadata);

	void markFailed(int logId, String errorMessage, Map<String, String> metadata);

	void markAsHardFailure(UUID orderId);

}
