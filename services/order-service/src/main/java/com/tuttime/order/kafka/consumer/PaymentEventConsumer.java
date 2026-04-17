package com.tuttime.order.kafka.consumer;

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

	private final OrderService orderService;
	private final PosGrpcClient posGrpcClient; // To push to the kitchen

	@KafkaListener(
			topics = TOPIC_PAYMENT_COMPLETED,
			groupId = "order-service-group")
	public void handlePaymentSuccess(PaymentEvent.PaymentCompletedEvent event) {
		log.info("Received payment success for booking: {}", event.bookingId());

		try {
			// 1. Mark the pre-order as PAID in the Order Service DB
			Order updatedOrder = orderService.confirmOrderPayment(event.bookingId());

			// 2. Send the order details to the restaurant's POS system (Kitchen)
			posGrpcClient.pushToKitchen(updatedOrder);

			log.info("Order for booking {} successfully pushed to POS.", event.bookingId());
		} catch (Exception e) {
			log.error("Failed to process order for booking: {}", event.bookingId(), e);
			// In production, handle retries or move to a Dead Letter Queue (DLQ)
		}
	}
}
