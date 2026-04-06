package com.tuttime.pos.kafka;

import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.PosEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PosEventProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final EventProducer eventProducer;

	public void publishSyncResult(String entityId, String entityType, String posSystem, String status, String message) {
		PosEvent.PosSyncedEvent event = new PosEvent.PosSyncedEvent(
				entityId,
				entityType,
				posSystem,
				status,
				message,
				OffsetDateTime.now()
		);
		eventProducer.sendPosSyncedEvent(event);
	}
}