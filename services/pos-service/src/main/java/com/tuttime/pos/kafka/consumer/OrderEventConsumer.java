package com.tuttime.pos.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.MenuEvent;
import com.tuttime.common.event.OrderEvent;
import com.tuttime.common.mapper.PosProtoMapper;
import com.tuttime.common.proto.OrderDetailResponse;
import com.tuttime.pos.dto.OrderSyncRequest;
import com.tuttime.pos.grpc.client.OrderGrpcClient;
import com.tuttime.pos.service.PosIntegrationService;
import com.tuttime.pos.service.PosSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

import static com.tuttime.common.event.EventProducer.TOPIC_ORDER_CREATED;
import static com.tuttime.common.event.EventProducer.TOPIC_ORDER_STATUS_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

	private final ObjectMapper objectMapper;
	private final PosSyncService posSyncService;
	private final PosIntegrationService posIntegrationService;
	private final OrderGrpcClient orderGrpcClient;

	@KafkaListener(
			topics = TOPIC_ORDER_CREATED,
			groupId = "pos-service-group")
	public void handleOrderCreated(String eventString) {
		OrderEvent.OrderCreatedEvent event = null;

		try {
			// 1. Handle Null Event
			if (!StringUtils.hasText(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, OrderEvent.OrderCreatedEvent.class);
			OrderEvent.OrderCreatedEvent finalEvent = event;

			OrderSyncRequest request = new OrderSyncRequest(finalEvent.orderId(), finalEvent.posSystem());
			posSyncService.syncOrder(request);
		} catch (Exception e) {
			// 5. Final safety net to prevent infinite retries of bad messages
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}

	@KafkaListener(
			topics = TOPIC_ORDER_STATUS_UPDATED,
			groupId = "pos-service-group")
	public void handleOrderStatusChange(String eventString) {
		OrderEvent.OrderStatusEvent event = null;

		try {
			// 1. Handle Null Event
			if (!StringUtils.hasText(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, OrderEvent.OrderStatusEvent.class);
			OrderEvent.OrderStatusEvent finalEvent = event;

			log.info("Received status update for Order {}: {} -> {}",
					finalEvent.orderId(), finalEvent.oldStatus(), finalEvent.newStatus());

			// Logic: Only send to physical POS if the order has been PAID
			if (finalEvent.newStatus() == OrderStatus.PAID) {
				log.info("Order {} is PAID. Pushing to restaurant POS system...", finalEvent.orderId());

				OrderDetailResponse details = orderGrpcClient.fetchOrderDetail(finalEvent.orderId());

				Map<String, Object> posPayload = PosProtoMapper.toPosMap(details);

				// Example: Dynamically accessing fields for a generic POS API
				log.info("Processing Order Map for POS: {}", posPayload.get("order_id"));
				// 2. Logic: Print or push to iiko/R-Keeper
				System.out.println("Printing Ticket for Order: " + details.getOrderId());
				details.getItemsList().forEach(item ->
						System.out.println(item.getQuantity() + "x " + item.getMenuItemName())
				);

				posIntegrationService.pushOrderToRestaurant(finalEvent.orderId());
			}
		} catch (Exception e) {
			// 5. Final safety net to prevent infinite retries of bad messages
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}

	@RetryableTopic(
			attempts = "4",                          // 1 initial + 3 retries
			backoff = @Backoff(delay = 5000, multiplier = 2.0), // Exponential backoff
			topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
	@KafkaListener(
			topics = TOPIC_ORDER_STATUS_UPDATED,
			groupId = "pos-service-group")
	public void handleWithRetry(String eventString) {
		OrderEvent.OrderStatusEvent event = null;
	}

}