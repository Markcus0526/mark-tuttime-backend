package com.tuttime.pos.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.kafka.event.PaymentEvent;
import com.tuttime.pos.adapter.PosAdapterFactory;
import com.tuttime.pos.repository.PosConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_PAYMENT_REFUND;

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
			groupId = "${spring.kafka.consumer.group-id}")
	public void handlePaymentRefunded(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			PaymentEvent.PaymentRefundedEvent event =
					objectMapper.readValue(eventString, PaymentEvent.PaymentRefundedEvent.class);

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
			log.error("<<< KAFKA[{}] >>> Consumer:[pos-service], Error: {}",
					TOPIC_PAYMENT_REFUND, e.getMessage());
		}
	}
}
