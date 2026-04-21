package com.tuttime.pos.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.OrderEvent;
import com.tuttime.common.mapper.PosProtoMapper;
import com.tuttime.common.proto.OrderDetailResponse;
import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.adapter.PosAdapterFactory;
import com.tuttime.pos.dto.PosSyncDto;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.grpc.client.OrderGrpcClient;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import com.tuttime.pos.service.PosIntegrationService;
import com.tuttime.pos.service.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Map;

import static com.tuttime.common.event.EventProducer.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

	private final ObjectMapper objectMapper;
	private final PosConfigRepository posConfigRepository;
	private final PosSyncQueueRepository posSyncQueueRepository;
	private final PosAdapterFactory posAdapterFactory;
	private final PosService posService;
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

			PosSyncDto.OrderSyncRequest request = new PosSyncDto.OrderSyncRequest(finalEvent.orderId(), finalEvent.branchId());
			posService.syncOrder(request);
		} catch (Exception e) {
			// 5. Final safety net to prevent infinite retries of bad messages
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}

	@KafkaListener(
			topics = TOPIC_ORDER_STATUS_UPDATED,
			groupId = "pos-service-group")
	public void handleOrderStatusChange(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			OrderEvent.OrderStatusEvent event = objectMapper.readValue(eventString, OrderEvent.OrderStatusEvent.class);

			// Logic: Only process when the order is PAID
			if (event.newStatus() == OrderStatus.PAID) {
				log.info("Processing PAID Order {} for POS sync", event.orderId());

				// 1. Fetch rich data via gRPC
				OrderDetailResponse details = orderGrpcClient.getOrderDetail(event.orderId());

				// 2. Fetch POS Config
				var config = posConfigRepository.findByBranchIdAndEnabledTrue(event.branchId())
						.orElseGet(() -> {
							log.warn("No active POS config found for branch {}", event.branchId());
							return null;
						});
				if (config == null) return;

				// 3. Get the correct Provider (iiko/R-Keeper)
				PosAdapter adapter = posAdapterFactory.getAdapter(config.getPosProvider());

				try {
					// 4. Try real-time sync immediately
					adapter.sendOrder(config, details);
					log.info("Order {} successfully sent to {} POS", event.orderId(), config.getPosProvider());

				} catch (Exception e) {
					log.error("Real-time sync failed for Order {}. Moving to Queue.", event.orderId());

					// 5. Failover: Save to pos_sync_queues for the background worker
					posSyncQueueRepository.save(PosSyncQueue.builder()
							.branchId(event.branchId())
							.orderId(event.orderId())
							.action("SEND_ORDER")
							.payload(Map.of("order_id", event.orderId())) // Minimal payload, worker will re-fetch if needed
							.status("PENDING")
							.nextRetryAt(OffsetDateTime.now().plusMinutes(1))
							.retryCount(0)
							.lastError(e.getMessage())
							.build());
				}
			}

			if (event.newStatus() == OrderStatus.CANCELED) {
				log.info("Order {} canceled. Notifying POS...", event.orderId());

				PosConfig config = posConfigRepository.findByBranchId(event.branchId())
						.orElseThrow(() -> new RuntimeException("Config not found"));

				PosAdapter adapter = posAdapterFactory.getAdapter(config.getPosProvider());
				adapter.cancelOrder(config, event.orderId(), "User requested cancellation");
			}
		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}