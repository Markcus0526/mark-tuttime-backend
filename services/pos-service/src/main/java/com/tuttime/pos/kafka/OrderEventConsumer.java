package com.tuttime.pos.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.kafka.event.OrderEvent;
import com.tuttime.common.event.proto.GetOrderDetailResponse;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.adapter.PosAdapterFactory;
import com.tuttime.pos.dto.PosSyncDto;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.PosLog;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.grpc.client.OrderGrpcClient;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.repository.PosLogRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import com.tuttime.pos.service.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_ORDER_CREATED;
import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_ORDER_STATUS_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final PosConfigRepository posConfigRepository;
    private final PosLogRepository posLogRepository;
    private final PosSyncQueueRepository posSyncQueueRepository;
    private final PosAdapterFactory posAdapterFactory;
    private final OrderGrpcClient orderGrpcClient;

    @KafkaListener(
            topics = TOPIC_ORDER_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory") // Wired to our Step 4 secure DLQ factory
    @Transactional
    public void handleOrderCreated(String eventString) {
        try {
            if (!StringUtils.hasText(eventString)) return;

            OrderEvent.OrderCreatedEvent event =
                    objectMapper.readValue(eventString, OrderEvent.OrderCreatedEvent.class);

            log.info("<<< KAFKA >>> Processing incoming kitchen ticket outbox initialization for Order ID: {}", event.orderId());

            // 2. Lookup the Branch Configuration
            PosConfig config = posConfigRepository.findByBranchIdAndEnabledTrue(event.branchId())
                    .orElseThrow(() -> new InvalidRequestException("Branch POS not configured or disabled for Branch: " + event.branchId()));

            // 3. Construct the Payload Map
            Map<String, Object> fullPayload = new HashMap<>();
            fullPayload.put("order_id", event.orderId().toString());
            fullPayload.put("branch_id", event.branchId().toString());
            fullPayload.put("occurred_at", event.occurredAt().toString());

            // 4. Save Audit Log (The local "Outbox" history marker)
            PosLog logEntry = PosLog.builder()
                    .branchId(config.getBranchId())
                    .entityId(event.orderId())
                    .entityType("ORDER")
                    .action("PUSH_TO_KITCHEN")
                    .requestPayload(fullPayload)
                    .status("PENDING")
                    .attemptCount(0)
                    .build();
            posLogRepository.save(logEntry);

            // 5. AUDIT GAP EXTENSION FIX: Initialize BOTH actions required by your pipeline scheduler

            // Task 1: Initialize the kitchen printer transmission task routing
            PosSyncQueue kitchenQueueEntry = PosSyncQueue.builder()
                    .branchId(event.branchId())
                    .orderId(event.orderId())
                    .action("SEND_TO_KITCHEN") // Picked up by case statement block 1 in your scheduler
                    .payload(fullPayload)
                    .status("PENDING")
                    .retryCount(0)
                    .nextRetryAt(OffsetDateTime.now())
                    .build();
            posSyncQueueRepository.save(kitchenQueueEntry);

            // Task 2: Initialize the enterprise financial system transmission routing task
            PosSyncQueue orderQueueEntry = PosSyncQueue.builder()
                    .branchId(event.branchId())
                    .orderId(event.orderId())
                    .action("SEND_ORDER") // Picked up by case statement block 2 in your scheduler
                    .payload(fullPayload)
                    .status("PENDING")
                    .retryCount(0)
                    .nextRetryAt(OffsetDateTime.now())
                    .build();
            posSyncQueueRepository.save(orderQueueEntry);

            log.info("Success! Local outbox queue records successfully populated for Order ID: {}", event.orderId());

        } catch (Exception e) {
            log.error("<<< KAFKA[tuttime.order.created] >>> Outbox processing failure: {}", e.getMessage());

            // CRITICAL STEP 4 DLQ FIX: Re-throw the exception!
            // If the database drops out here, this re-throw will make Kafka retry or pass the message to your backup queue.
            throw new RuntimeException("POS Service consumer failed to save outbox sync queue", e);
        }
    }

    @KafkaListener(
            topics = TOPIC_ORDER_STATUS_UPDATED,
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleOrderStatusChange(String eventString) {
        try {
            if (!StringUtils.hasText(eventString)) return;

            OrderEvent.OrderStatusEvent event =
                    objectMapper.readValue(eventString, OrderEvent.OrderStatusEvent.class);

            // Logic: Only process when the order is CREATED
            if (event.newStatus() == OrderStatus.CREATED) {

                // 1. Fetch rich data via gRPC
                GetOrderDetailResponse details = orderGrpcClient.getOrderDetail(event.orderId());

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
                    adapter.sendOrder(config, event.orderId());
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
                PosConfig config = posConfigRepository.findByBranchId(event.branchId())
                        .orElseThrow(() -> new RuntimeException("Config not found"));

                PosAdapter adapter = posAdapterFactory.getAdapter(config.getPosProvider());
                adapter.cancelOrder(config, event.orderId(), "User requested cancellation");
            }
        } catch (Exception e) {
            log.error("<<< KAFKA[{}] >>> Consumer:[pos-service], Error: {}",
                    TOPIC_ORDER_STATUS_UPDATED, e.getMessage());
        }
    }
}