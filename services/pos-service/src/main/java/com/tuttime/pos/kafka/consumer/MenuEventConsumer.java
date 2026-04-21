package com.tuttime.pos.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.MenuEvent;
import com.tuttime.pos.dto.PosSyncDto;
import com.tuttime.pos.service.PosService;
import com.tuttime.pos.service.PosSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tuttime.common.event.EventProducer.TOPIC_MENU_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuEventConsumer {

	private final ObjectMapper objectMapper;
	private final PosService posService;

	@KafkaListener(
			topics = TOPIC_MENU_UPDATED,
			groupId = "pos-service-group")
	public void handleMenuUpdated(String eventString) {
		MenuEvent.MenuUpdatedEvent event = null;

		try {
			// 1. Handle Null Event
			if (!StringUtils.hasText(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, MenuEvent.MenuUpdatedEvent.class);
			MenuEvent.MenuUpdatedEvent finalEvent = event;

			PosSyncDto.MenuSyncRequest request = new PosSyncDto.MenuSyncRequest(finalEvent.branchId());
			posService.syncMenu(request);

		} catch (Exception e) {
			// 5. Final safety net to prevent infinite retries of bad messages
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}
