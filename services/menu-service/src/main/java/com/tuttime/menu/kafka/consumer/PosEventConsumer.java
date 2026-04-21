package com.tuttime.menu.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.BookingEvent;
import com.tuttime.common.event.PosEvent;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tuttime.common.event.EventProducer.TOPIC_POS_ITEMS_UPDATED;
import static com.tuttime.common.event.EventProducer.TOPIC_POS_PRICE_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosEventConsumer {

	private final ObjectMapper objectMapper;
	private final MenuItemRepository menuItemRepository;
	private final MenuItemService menuItemService;
//	private final MenuCacheService menuCacheService; TODO

	@KafkaListener(
			topics = TOPIC_POS_ITEMS_UPDATED,
			groupId = "menu-service-group")
	public void handlePosMenuUpdate(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			PosEvent.PosMenuUpdatedEvent event = objectMapper.readValue(eventString, PosEvent.PosMenuUpdatedEvent.class);
			if (event.itemIds() == null || event.itemIds().isEmpty()) {
				log.debug("Received empty POS menu update for branch {}", event.branchId());
				return;
			}

			log.info("Received {} event for branch {}: {} items",
					event.updateType(), event.branchId(), event.itemIds().size());

			boolean isAvailable = "ACTIVATION".equals(event.updateType());

			// Call the service we just updated
			menuItemService.updateAvailabilityFromPos(
					event.itemIds(),
					isAvailable,
					event.branchId()
			);

		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}

	@KafkaListener(
			topics = TOPIC_POS_PRICE_UPDATED,
			groupId = "menu-service-group")
	public void handlePriceUpdate(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			PosEvent.PosPriceUpdatedEvent event = objectMapper.readValue(eventString, PosEvent.PosPriceUpdatedEvent.class);

			log.info("Updating price for item {} to {}", event.menuItemId(), event.newPrice());

			menuItemService.updatePriceFromPos(
					event.menuItemId(),
					event.newPrice(),
					event.branchId()
			);

		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}
