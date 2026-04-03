package com.tuttime.pos.kafka.consumer;

import com.tuttime.common.event.MenuEvent;
import com.tuttime.pos.dto.OrderSyncRequest;
import com.tuttime.pos.service.PosSyncService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.tuttime.common.event.EventProducer.TOPIC_MENU_UPDATED;

@Service
@RequiredArgsConstructor
public class MenuEventConsumer {

	private final PosSyncService posSyncService;

	@KafkaListener(
			topics = TOPIC_MENU_UPDATED,
			groupId = "pos-service-group",
			containerFactory = "menuUpdatedKafkaListenerContainerFactory")
	public void consumeMenuUpdated(ConsumerRecord<String, MenuEvent.MenuUpdatedEvent> record) {
		MenuEvent.MenuUpdatedEvent event = record.value();
		OrderSyncRequest request = new OrderSyncRequest(UUID.fromString(event.restaurantId()), event.posSystem());
		posSyncService.syncOrder(request);
	}
}
