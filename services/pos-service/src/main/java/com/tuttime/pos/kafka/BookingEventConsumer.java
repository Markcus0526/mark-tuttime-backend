package com.tuttime.pos.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.adapter.PosAdapterFactory;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_BOOKING_STATUS_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final ObjectMapper objectMapper;
    private final PosConfigRepository posConfigRepository;
    private final PosSyncQueueRepository posSyncQueueRepository;
    private final PosAdapterFactory posAdapterFactory;

    @KafkaListener(
            topics = TOPIC_BOOKING_STATUS_UPDATED,
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional(readOnly = true)
    public void handleBookingStatusUpdated(String eventString) {
        try {
            if (!StringUtils.hasText(eventString)) return;

            BookingEvent.BookingStatusUpdatedEvent event =
                    objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);

            List<UUID> tableIds = extractTableIds(event);
            if (tableIds.isEmpty()) {
                log.warn("No table IDs in event for booking {}", event.bookingId());
                return;
            }

            // 1. Look up the POS configuration for this specific branch
            var config = posConfigRepository.findByBranchIdAndEnabledTrue(event.branchId())
                    .orElseGet(() -> {
                        log.warn("No active POS config found for branch {}", event.branchId());
                        return null;
                    });
            if (config == null) return;

            // 2. Get the correct provider (iiko/R-Keeper) via the Factory
            PosAdapter adapter = posAdapterFactory.getAdapter(config.getPosProvider());

            // 3. Execute the corresponding POS action
            try {
                switch (event.newStatus()) {
                    case PAID -> reserveTables(adapter, config, tableIds, event.bookingId());
                    case ARRIVED -> occupyTables(adapter, config, tableIds, event.bookingId());
                    case COMPLETED, CANCELED, NO_SHOW -> releaseTables(adapter, config, tableIds, event.bookingId());
                    default -> log.debug("No POS action mapped for status {}", event.newStatus());
                }
            } catch (Exception e) {
                // Log is already saved inside the adapter; here we handle the application-level failure
                log.error("Failed to sync booking {} to {} POS",
                        event.bookingId(), config.getPosProvider(), e);

                // Optional: Re-throw to trigger Kafka Retry DLT or move to your pos_sync_queues
                handleSyncFailure(event, tableIds, e);
            }
        } catch (Exception e) {
            // 5. Final safety net to prevent infinite retries of bad messages
            log.error("<<< KAFKA[{}] >>> Consumer:[pos-service], Error: {}",
                    TOPIC_BOOKING_STATUS_UPDATED, e.getMessage());
        }
    }

    // Helper: extract list of table IDs from event
    private List<UUID> extractTableIds(BookingEvent.BookingStatusUpdatedEvent event) {
        if (event.tableIds() != null && !event.tableIds().isEmpty()) {
            return event.tableIds();
        }
        return Collections.emptyList();
    }

    // Batch reserve
    private void reserveTables(PosAdapter adapter, PosConfig config, List<UUID> tableIds, UUID bookingId) {
        for (UUID tableId : tableIds) {
            adapter.reserveTable(config, tableId, bookingId);
        }
    }

    // Batch occupy
    private void occupyTables(PosAdapter adapter, PosConfig config, List<UUID> tableIds, UUID bookingId) {
        for (UUID tableId : tableIds) {
            adapter.occupyTable(config, tableId, bookingId);
        }
    }

    // Batch release
    private void releaseTables(PosAdapter adapter, PosConfig config, List<UUID> tableIds, UUID bookingId) {
        for (UUID tableId : tableIds) {
            adapter.releaseTable(config, tableId, bookingId);
        }
    }

    // Queue the whole booking (all tables) for retry
    private void handleSyncFailure(BookingEvent.BookingStatusUpdatedEvent event, List<UUID> tableIds, Exception e) {
        log.info("Queuing failed sync for booking {} (tables: {}) to retry later", event.bookingId(), tableIds);

        PosSyncQueue queueItem = new PosSyncQueue();
        queueItem.setBranchId(event.branchId());
        queueItem.setBookingId(event.bookingId());

        // Store table IDs as comma-separated string (or JSON)
        String tableIdsStr = tableIds.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));
        queueItem.setTableIds(tableIdsStr);

        // Determine action name
        String action = switch (event.newStatus()) {
            case PAID -> "RESERVE_TABLE";
            case ARRIVED -> "OCCUPY_TABLE";
            default -> "RELEASE_TABLE";
        };
        queueItem.setAction(action);
        queueItem.setLastError(e.getMessage());
        queueItem.setNextRetryAt(OffsetDateTime.now().plusMinutes(1));
        queueItem.setRetryCount(1);
        posSyncQueueRepository.save(queueItem);
    }
}