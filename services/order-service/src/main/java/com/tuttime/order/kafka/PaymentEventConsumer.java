package com.tuttime.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.kafka.event.PaymentEvent;
import com.tuttime.order.entity.Order;
import com.tuttime.order.grpc.client.PosGrpcClient;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_PAYMENT_COMPLETED;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

	private final ObjectMapper objectMapper;
	private final OrderService orderService;
	private final PosGrpcClient posGrpcClient; // To push to the kitchen

	@KafkaListener(
			topics = TOPIC_PAYMENT_COMPLETED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handlePaymentSuccess(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			PaymentEvent.PaymentCompletedEvent event =
					objectMapper.readValue(eventString, PaymentEvent.PaymentCompletedEvent.class);

			// 1. Mark as CREATED and get the full order (including items for the POS)
			Order updatedOrder = orderService.createOrUpdateOrder(event.bookingId(), event.userId(), event.branchId());

			// 2. IDEMPOTENCY CHECK: Only push if it hasn't been sent yet
			if (updatedOrder.getStatus() == OrderStatus.CREATED) {
				posGrpcClient.pushToKitchen(updatedOrder);

				// 3. Move status to SENT_TO_RESTAURANT immediately after successful push
				orderService.updateStatusFromPayment(updatedOrder.getId(), OrderStatus.SENT_TO_RESTAURANT);
			}

		} catch (Exception e) {
			log.error("<<< KAFKA >>> Consumer Error: {}: {}",
					TOPIC_PAYMENT_COMPLETED, e.getMessage());
		}
	}
}
