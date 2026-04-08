package com.tuttime.pos.kafka.consumer;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.OrderEvent;
import com.tuttime.common.mapper.PosProtoMapper;
import com.tuttime.common.proto.OrderDetailsResponse;
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

import java.util.Map;
import java.util.UUID;

import static com.tuttime.common.event.EventProducer.TOPIC_ORDER_CREATED;
import static com.tuttime.common.event.EventProducer.TOPIC_ORDER_STATUS_UPDATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

	private final PosSyncService posSyncService;
	private final PosIntegrationService posIntegrationService;
	private final OrderGrpcClient orderGrpcClient;

	@KafkaListener(
			topics = TOPIC_ORDER_CREATED,
			groupId = "pos-service-group")
	public void consumeOrderCreated(ConsumerRecord<String, OrderEvent.OrderCreatedEvent> record) {
		OrderEvent.OrderCreatedEvent event = record.value();
		OrderSyncRequest request = new OrderSyncRequest(UUID.fromString(event.orderId()), event.posSystem());
		posSyncService.syncOrder(request);
	}

	@KafkaListener(
			topics = TOPIC_ORDER_STATUS_UPDATED,
			groupId = "pos-service-group")
	public void handleOrderStatusChange(OrderEvent.OrderStatusEvent event) {
		log.info("Received status update for Order {}: {} -> {}",
				event.orderId(), event.oldStatus(), event.newStatus());

		// Logic: Only send to physical POS if the order has been PAID
		if (event.newStatus() == OrderStatus.PAID) {
			log.info("Order {} is PAID. Pushing to restaurant POS system...", event.orderId());

			OrderDetailsResponse details = orderGrpcClient.fetchOrderDetails(event.orderId());

			try {
				// Convert to Map
				Map<String, Object> posPayload = PosProtoMapper.toPosMap(details);

				// Example: Dynamically accessing fields for a generic POS API
				log.info("Processing Order Map for POS: {}", posPayload.get("order_id"));
				// 2. Logic: Print or push to iiko/R-Keeper
				System.out.println("Printing Ticket for Order: " + details.getOrderId());
				details.getItemsList().forEach(item ->
						System.out.println(item.getQuantity() + "x " + item.getMenuItemName())
				);

				posIntegrationService.pushOrderToRestaurant(event.orderId());

			} catch (Exception e) {
				log.error("Failed to convert gRPC response to Map", e);
			}
		}
	}

	@RetryableTopic(
			attempts = "4",                          // 1 initial + 3 retries
			backoff = @Backoff(delay = 5000, multiplier = 2.0), // Exponential backoff
			topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
	@KafkaListener(
			topics = TOPIC_ORDER_STATUS_UPDATED,
			groupId = "pos-service-group")
	public void consumeWithRetry(OrderEvent.OrderStatusEvent event) {
		// ... same logic as above
	}

	@DltHandler
	public void handleDlt(OrderEvent.OrderStatusEvent event) {
		log.error("Order {} failed all retries. Manual intervention required.", event.orderId());
		// Logic: Mark the DB PosLog as 'FAILED' for the Admin dashboard to see
		posSyncService.markAsHardFailure(event.orderId());
	}
}