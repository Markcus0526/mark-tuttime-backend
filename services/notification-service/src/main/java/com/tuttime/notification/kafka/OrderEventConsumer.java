package com.tuttime.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.event.kafka.event.OrderEvent;
import com.tuttime.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_ORDER_CREATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationOrchestrator notificationOrchestrator;

    /**
     * Handles Order Created notifications.
     * Triggered automatically after an order is initialized from a successful payment.
     */
    @KafkaListener(
            topics = TOPIC_ORDER_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory") // Wired to our Step 4 secure DLQ factory
    public void handleOrderCreated(String eventString) {
        try {
            if (!StringUtils.hasText(eventString)) return;

            // 1. Unpack your exact OrderEvent.OrderCreatedEvent record model
            OrderEvent.OrderCreatedEvent event =
                    objectMapper.readValue(eventString, OrderEvent.OrderCreatedEvent.class);

            log.info("<<< KAFKA >>> Processing notification template context mapping for Order ID: {}", event.orderId());

            // 2. Map structural event data metrics to template context parameters
            Map<String, Object> params = Map.of(
                    "orderId", event.orderId().toString(),
                    "branchId", event.branchId().toString(),
                    "occurredAt", event.occurredAt().toString()
            );

            // 3. Match the target template tracking key (Added in Step 7)
            NotificationTemplateKey templateKey = NotificationTemplateKey.ORDER_CREATED;

            // 4. Delegate task directly to your notification engine orchestration hub
            // Note: If you need to map user emails, pass user context details inside your orchestrator layers
            notificationOrchestrator.handleNotification(
                    event.eventId(),
                    null, // Pass the explicit userId parameter if added to your core event structures later
                    templateKey,
                    params);

        } catch (Exception e) {
            log.error("<<< KAFKA[tuttime.order.created] >>> Consumer:[notification-service], Error: {}", e.getMessage());

            // CRITICAL STEP 4 DLQ FIX: Re-throw the exception!
            // This prevents silent message dropouts and sends failed events straight to your cluster backup queue.
            throw new RuntimeException("Notification pipeline processing error occurred for order stream", e);
        }
    }
}
