package com.tuttime.pos.service;

import com.tuttime.pos.dto.*;
import com.tuttime.pos.entity.PosSystemConfig;

import java.util.Map;
import java.util.UUID;

public interface PosSyncService {
	void syncBooking(BookingSyncRequest request);
	void syncOrder(OrderSyncRequest request);
	void syncMenu(MenuSyncRequest request);

	PosSyncLogResponse getSyncLogs(UUID entityId);
	PosSystemConfig saveConfig(PosCredentialsDto dto);
	void forceSyncByRestaurantId(UUID restaurantId);

	void markSuccess(int logId, Map<String, String> metadata);
	void markFailed(int logId, String errorMessage, Map<String, String> metadata);
	void markAsHardFailure(UUID orderId);
}
