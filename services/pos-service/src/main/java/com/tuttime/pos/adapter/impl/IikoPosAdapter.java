package com.tuttime.pos.adapter.impl;

import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.client.IikoApiClient;
import com.tuttime.pos.dto.liko.booking.IikoBookingDTO;
import com.tuttime.pos.dto.liko.menu.IikoMenuCategoryDTO;
import com.tuttime.pos.dto.liko.menu.IikoMenuDTO;
import com.tuttime.pos.dto.liko.menu.IikoMenuItemDTO;
import com.tuttime.pos.dto.liko.order.IikoModifierDTO;
import com.tuttime.pos.dto.liko.order.IikoOrderDTO;
import com.tuttime.pos.dto.liko.order.IikoOrderItemDTO;
import com.tuttime.pos.entity.PosLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component("iiko")
@RequiredArgsConstructor
public class IikoPosAdapter implements PosAdapter {

	private final IikoApiClient iikoApiClient;
	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	private static final Logger logger = LoggerFactory.getLogger(IikoPosAdapter.class);

	@Override
	public String getPosSystemName() {
		return "iiko";
	}

	@Override
	public void syncBooking(PosLog posLog) {
		logger.info("Syncing booking with Iiko POS for entityId: {}", posLog.getEntityId());
		// TODO: Implement Iiko-specific API calls for booking sync
//		Map<String, Object> bookingPayload = mapBookingToIikoPayload(posLog);
//		iikoApiClient.syncBooking(bookingPayload).block(); // block() for sync call, or handle reactive properly
	}

	@Override
	public void syncOrder(PosLog posLog) {
		logger.info("Syncing order with Iiko POS for entityId: {}", posLog.getEntityId());
		// TODO: Implement Iiko-specific API calls for order sync
//		Map<String, Object> orderPayload = mapOrderToIikoPayload(posLog);
//		iikoApiClient.syncOrder(orderPayload).block();
	}

	@Override
	public void syncMenu(PosLog posLog) {
		logger.info("Syncing menu with Iiko POS for entityId: {}", posLog.getEntityId());
		// TODO: Implement Iiko-specific API calls for menu sync
//		Map<String, Object> menuPayload = mapMenuToIikoPayload(posLog);
//		iikoApiClient.syncMenu(menuPayload).block();
	}

	@Override
	public boolean syncEntity(String entityType, UUID entityId) {
		// Implement actual sync logic with iiko POS API
		// Return true if success, false if failure
		return true; // dummy implementation
	}

	// TODO: Implement mapping methods from PosLog to API payloads
	private IikoBookingDTO mapBookingToIikoPayload(PosLog posLog) {
		// Example dummy mapping, replace with actual data extraction from posLog or associated entities
		return IikoBookingDTO.builder()
				.bookingId(posLog.getEntityId().toString())
				.customerName("John Doe") // fetch real customer name
				.customerPhone("1234567890") // fetch real phone
				.restaurantId("rest123")
				.tableId("table456")
				.startTime(ISO_FORMATTER.format(posLog.getLastAttempt().atZoneSameInstant(ZoneOffset.UTC)))
				.endTime(ISO_FORMATTER.format(posLog.getLastAttempt().plusSeconds(3600).atZoneSameInstant(ZoneOffset.UTC)))
				.guests(4)
				.build();
	}

	private IikoOrderDTO mapOrderToIikoPayload(PosLog posLog) {
		// Build order with items and modifiers
		IikoOrderItemDTO item = IikoOrderItemDTO.builder()
				.menuItemId("menu789")
				.quantity(2)
				.price(12.50)
				.modifiers(List.of(new IikoModifierDTO("mod1", 1.5)))
				.build();

		return IikoOrderDTO.builder()
				.orderId(posLog.getEntityId().toString())
				.bookingId("booking123")
				.restaurantId("rest123")
				.items(List.of(item))
				.totalAmount(26.0)
				.orderStatus("CONFIRMED")
				.build();
	}

	private IikoMenuDTO mapMenuToIikoPayload(PosLog posLog) {
		IikoMenuItemDTO menuItem = IikoMenuItemDTO.builder()
				.menuItemId("menu789")
				.name("Pizza Margherita")
				.price(12.50)
				.modifiers(List.of(new IikoModifierDTO("mod1", 1.5)))
				.build();

		IikoMenuCategoryDTO category = IikoMenuCategoryDTO.builder()
				.categoryId("cat1")
				.name("Pizzas")
				.items(List.of(menuItem))
				.build();

		return IikoMenuDTO.builder()
				.restaurantId("rest123")
				.categories(List.of(category))
				.build();
	}
}
