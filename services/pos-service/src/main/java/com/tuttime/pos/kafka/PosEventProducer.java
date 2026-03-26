package com.tuttime.pos.kafka;

import com.tuttime.common.kafka.PosSyncResultEvent;
import com.tuttime.pos.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PosEventProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public void publishSyncResult(String entityId, String entityType, String posSystem, String status, String message) {
		PosSyncResultEvent event = new PosSyncResultEvent(
				entityId,
				entityType,
				posSystem,
				status,
				message,
				Instant.now()
		);
		kafkaTemplate.send(KafkaConfig.POS_SYNC_RESULT_TOPIC, entityId, event);
	}
}