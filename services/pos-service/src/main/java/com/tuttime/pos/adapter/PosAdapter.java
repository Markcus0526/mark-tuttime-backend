package com.tuttime.pos.adapter;

import com.tuttime.common.proto.OrderDetailResponse;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.enums.PosProvider;

import java.math.BigDecimal;
import java.util.UUID;

public interface PosAdapter {

	/**
	 * Maps to BookingStatus.PAID
	 * Tells the POS to mark a table as "Reserved" for a specific time.
	 */
	void reserveTable(PosConfig config, UUID tableId, UUID bookingId);

	/**
	 * Maps to BookingStatus.ARRIVED
	 * Tells the POS a guest is present. Often opens a "session" or "order" on the table.
	 */
	void occupyTable(PosConfig config, UUID tableId, UUID bookingId);

	/**
	 * Maps to BookingStatus.CANCELED, NO_SHOW, or COMPLETED
	 * Releases the table back to "Available" in the POS layout.
	 */
	void releaseTable(PosConfig config, UUID tableId, UUID bookingId);

	/**
	 * NEW: Maps to OrderStatus.PAID
	 * Sends the full ticket (items, quantity, price) to the POS kitchen/terminal.
	 * Uses the gRPC OrderDetailResponse for rich data.
	 */
	void sendOrder(PosConfig config, OrderDetailResponse order);
	void cancelOrder(PosConfig config, UUID orderId, String reason);
	void refundOrder(PosConfig config, UUID orderId, BigDecimal amount, String reason);

	/**
	 * NEW: Fetches the latest nomenclature/menu from the POS.
	 * This updates the mapping table with external IDs and prices.
	 */
	void syncMenu(PosConfig config);

	/**
	 * Returns the provider this adapter supports (e.g., IIKO, RKEEPER).
	 */
	PosProvider getProvider();
}
