package com.tuttime.pos.adapter.impl;

import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.dto.rkeeper.booking.RKeeperBookingDTO;
import com.tuttime.pos.dto.rkeeper.menu.RKeeperMenuCategoryDTO;
import com.tuttime.pos.dto.rkeeper.menu.RKeeperMenuDTO;
import com.tuttime.pos.dto.rkeeper.menu.RKeeperMenuItemDTO;
import com.tuttime.pos.dto.rkeeper.order.RKeeperModifierDTO;
import com.tuttime.pos.dto.rkeeper.order.RKeeperOrderDTO;
import com.tuttime.pos.dto.rkeeper.order.RKeeperOrderItemDTO;
import com.tuttime.pos.entity.PosLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component("rkeeper")
@RequiredArgsConstructor
public class RKeeperPosAdapter implements PosAdapter {

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	private static final Logger logger = LoggerFactory.getLogger(RKeeperPosAdapter.class);

	// Inject any necessary services or clients here

	@Override
	public String getPosSystemName() {
		return "rkeeper";
	}

	@Override
	public void syncBooking(PosLog posLog) {
		logger.info("Syncing booking with R-Keeper POS for entityId: {}", posLog.getEntityId());
		// TODO: Implement R-Keeper-specific API calls for booking sync
	}

	@Override
	public void syncOrder(PosLog posLog) {
		logger.info("Syncing order with R-Keeper POS for entityId: {}", posLog.getEntityId());
		// TODO: Implement R-Keeper-specific API calls for order sync
	}

	@Override
	public void syncMenu(PosLog posLog) {
		logger.info("Syncing menu with R-Keeper POS for entityId: {}", posLog.getEntityId());
		// TODO: Implement R-Keeper-specific API calls for menu sync
	}

	@Override
	public boolean syncEntity(String entityType, UUID entityId) {
		return false;
	}

	private RKeeperBookingDTO mapBookingToRKeeper(PosLog posLog) {
		return RKeeperBookingDTO.builder()
				.reservationId(posLog.getEntityId().toString())
				.clientName("Jane Smith")
				.clientPhone("0987654321")
				.restaurantCode("rk_rest_001")
				.tableLabel("T12")
				.reservationStart(ISO_FORMATTER.format(posLog.getLastAttempt().atZoneSameInstant(ZoneOffset.UTC)))
				.reservationEnd(ISO_FORMATTER.format(posLog.getLastAttempt().plusSeconds(5400).atZoneSameInstant(ZoneOffset.UTC)))
				.personsCount(3)
				.build();
	}

	private RKeeperOrderDTO mapOrderToRKeeper(PosLog posLog) {
		RKeeperOrderItemDTO item = RKeeperOrderItemDTO.builder()
				.productCode("prod123")
				.quantity(1)
				.price(15.0)
				.modifiers(List.of(new RKeeperModifierDTO("modA", 2.0)))
				.build();

		return RKeeperOrderDTO.builder()
				.orderCode(posLog.getEntityId().toString())
				.reservationId("resv001")
				.restaurantCode("rk_rest_001")
				.orderItems(List.of(item))
				.totalSum(15.0)
				.status("NEW")
				.build();
	}

	private RKeeperMenuDTO mapMenuToRKeeper(PosLog posLog) {
		RKeeperMenuItemDTO menuItem = RKeeperMenuItemDTO.builder()
				.productCode("prod123")
				.productName("Cheeseburger")
				.basePrice(8.0)
				.modifiers(List.of(new RKeeperModifierDTO("modA", 1.0)))
				.build();

		RKeeperMenuCategoryDTO category = RKeeperMenuCategoryDTO.builder()
				.categoryCode("cat01")
				.categoryName("Burgers")
				.menuItems(List.of(menuItem))
				.build();

		return RKeeperMenuDTO.builder()
				.restaurantCode("rk_rest_001")
				.menuCategories(List.of(category))
				.build();
	}
}
