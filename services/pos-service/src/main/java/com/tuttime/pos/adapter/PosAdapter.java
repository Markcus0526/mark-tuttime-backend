package com.tuttime.pos.adapter;

import com.tuttime.pos.entity.PosLog;

import java.util.UUID;

public interface PosAdapter {
	String getPosSystemName();
	void syncBooking(PosLog posLog);
	void syncOrder(PosLog posLog);
	void syncMenu(PosLog posLog);

	/**
	 * Sync entity data with the external POS system.
	 * @param entityType Type of the entity: BOOKING, ORDER, MENU
	 * @param entityId ID of the entity to sync
	 * @return true if sync succeeded, false otherwise
	 */
	boolean syncEntity(String entityType, UUID entityId);
}
