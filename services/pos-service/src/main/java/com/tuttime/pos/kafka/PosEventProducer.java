package com.tuttime.pos.kafka;

import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.PosEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosEventProducer {

	private final EventProducer eventProducer;

	public void sendActivationEvent(UUID branchId, List<UUID> itemIds) {
		if (itemIds.isEmpty()) return;

		PosEvent.PosMenuUpdatedEvent event = new PosEvent.PosMenuUpdatedEvent(
				branchId,
				itemIds,
				"ACTIVATION", // Type discriminator
				OffsetDateTime.now()
		);

		log.info("Publishing activation event for {} items in branch {}", itemIds.size(), branchId);

		eventProducer.sendPosMenuUpdatedEvent(event);
	}

	public void sendDeactivationEvent(UUID branchId, List<UUID> itemIds) {
		if (itemIds.isEmpty()) return;

		PosEvent.PosMenuUpdatedEvent event = new PosEvent.PosMenuUpdatedEvent(
				branchId,
				itemIds,
				"DEACTIVATION",
				OffsetDateTime.now()
		);

		log.info("Publishing deactivation event for {} items in branch {}", itemIds.size(), branchId);

		// Sending with branchId as key for partition ordering
		eventProducer.sendPosMenuUpdatedEvent(event);
	}

	public void sendPriceUpdatedEvent(UUID branchId, UUID menuItemId, BigDecimal newPrice) {
		PosEvent.PosPriceUpdatedEvent event = new PosEvent.PosPriceUpdatedEvent(
				branchId,
				menuItemId,
				newPrice,
				OffsetDateTime.now()
		);

		log.info("Publishing new price {} event for {} items in branch {}", newPrice, menuItemId, branchId);

		// Sending with branchId as key for partition ordering
		eventProducer.sendPosPriceUpdatedEvent(event);
	}
}