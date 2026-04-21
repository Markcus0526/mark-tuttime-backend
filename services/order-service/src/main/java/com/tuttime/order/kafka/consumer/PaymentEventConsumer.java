package com.tuttime.order.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.PaymentEvent;
import com.tuttime.order.entity.Order;
import com.tuttime.order.grpc.client.PosGrpcClient;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.tuttime.common.event.EventProducer.TOPIC_PAYMENT_COMPLETED;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

	private final ObjectMapper objectMapper;
	private final OrderService orderService;
	private final PosGrpcClient posGrpcClient; // To push to the kitchen

	@KafkaListener(
			topics = TOPIC_PAYMENT_COMPLETED,
			groupId = "order-service-group")
	public void handlePaymentSuccess(String eventString) {
		try {
			PaymentEvent.PaymentCompletedEvent event = objectMapper.readValue(eventString, PaymentEvent.PaymentCompletedEvent.class);

			// 1. Mark as PAID and get the full order (including items for the POS)
			Order updatedOrder = orderService.confirmOrderPayment(event.bookingId(), event.userId(), event.branchId());

			// 2. IDEMPOTENCY CHECK: Only push if it hasn't been sent yet
			if (updatedOrder.getStatus() == OrderStatus.PAID) {
				posGrpcClient.pushToKitchen(updatedOrder);

				// 3. Move status to SENT_TO_RESTAURANT immediately after successful push
				orderService.updateStatus(updatedOrder.getId(), OrderStatus.SENT_TO_RESTAURANT);
				log.info("Order for booking {} pushed to POS.", event.bookingId());
			}

		} catch (Exception e) {
			log.error("Payment Success Consumer Error: {}", e.getMessage());
		}
	}
}
