package com.tuttime.pos.service;

import com.tuttime.pos.dto.BookingSyncRequest;
import com.tuttime.pos.dto.MenuSyncRequest;
import com.tuttime.pos.dto.OrderSyncRequest;
import com.tuttime.pos.dto.PosSyncLogResponse;

public interface PosSyncService {

	void syncBooking(BookingSyncRequest request);

	void syncOrder(OrderSyncRequest request);

	void syncMenu(MenuSyncRequest request);

	PosSyncLogResponse getSyncLogs(String entityId);

	void processRetryQueue();
}