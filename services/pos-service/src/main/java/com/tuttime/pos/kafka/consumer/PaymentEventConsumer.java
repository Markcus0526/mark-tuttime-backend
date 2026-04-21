package com.tuttime.pos.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.PaymentEvent;
import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.adapter.PosAdapterFactory;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.repository.PosConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.tuttime.common.event.EventProducer.TOPIC_PAYMENT_COMPLETED;
import static com.tuttime.common.event.EventProducer.TOPIC_PAYMENT_REFUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

	private final ObjectMapper objectMapper;
	private final PosConfigRepository posConfigRepository;
	private final PosAdapterFactory posAdapterFactory;

	/**
	 * Cancels the booking and frees the table if a refund occurs.
	 */
	@KafkaListener(
			topics = TOPIC_PAYMENT_REFUND,
			groupId = "pos-service-group")
	public void handlePaymentRefunded(String eventString) {
		PaymentEvent.PaymentRefundedEvent event = null;

		try {
			if (org.apache.commons.lang3.StringUtils.isEmpty(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, PaymentEvent.PaymentRefundedEvent.class);
			PaymentEvent.PaymentRefundedEvent finalEvent = event;

//			if ("REFUND_COMPLETED".equals(finalEvent.getType())) {
//				log.info("Processing refund sync for Order: {}", finalEvent.getOrderId());
//
//				PosConfig config = posConfigRepository.findByBranchId(finalEvent.getBranchId())
//						.orElseThrow(() -> new RuntimeException("Config not found"));
//
//				PosAdapter adapter = posAdapterFactory.getAdapter(config.getPosProvider());
//				adapter.refundOrder(config, finalEvent.getOrderId(), finalEvent.getAmount(), "Payment Gateway Refund");
//			}TODO
		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}
