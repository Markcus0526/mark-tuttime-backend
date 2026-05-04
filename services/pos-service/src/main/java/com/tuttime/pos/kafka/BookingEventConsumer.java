package com.tuttime.pos.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.pos.adapter.PosAdapter;
import com.tuttime.pos.adapter.PosAdapterFactory;
import com.tuttime.pos.entity.PosSyncQueue;
import com.tuttime.pos.repository.PosConfigRepository;
import com.tuttime.pos.repository.PosSyncQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

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
	public void handleBookingStatusUpdated(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			BookingEvent.BookingStatusUpdatedEvent event =
					objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);

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
					case PAID -> adapter.reserveTable(config, event.tableId(), event.bookingId());

					case ARRIVED -> adapter.occupyTable(config, event.tableId(), event.bookingId());

					case COMPLETED, CANCELED, NO_SHOW ->
							adapter.releaseTable(config, event.tableId(), event.bookingId());

					default -> log.debug("No POS action mapped for status {}", event.newStatus());
				}
			} catch (Exception e) {
				// Log is already saved inside the adapter; here we handle the application-level failure
				log.error("Failed to sync booking {} to {} POS",
						event.bookingId(), config.getPosProvider(), e);

				// Optional: Re-throw to trigger Kafka Retry DLT or move to your pos_sync_queues
				handleSyncFailure(event, e);
			}
		} catch (Exception e) {
			// 5. Final safety net to prevent infinite retries of bad messages
			log.error("<<< KAFKA[{}] >>> Consumer:[pos-service], Error: {}",
					TOPIC_BOOKING_STATUS_UPDATED, e.getMessage());
		}
	}

	private void handleSyncFailure(BookingEvent.BookingStatusUpdatedEvent event, Exception e) {
		log.info("Queuing failed sync for booking {} to retry later", event.bookingId());

		PosSyncQueue queueItem = new PosSyncQueue();
		queueItem.setBranchId(event.branchId());
		queueItem.setBookingId(event.bookingId());

		// Map the status to the action name for the retry task
		String action = switch (event.newStatus()) {
			case PAID -> "RESERVE_TABLE";
			case ARRIVED -> "OCCUPY_TABLE";
			default -> "RELEASE_TABLE";
		};

		queueItem.setAction(action);
		queueItem.setLastError(e.getMessage());

		// Exponential Backoff: Try again in 1 minute for the first retry
		queueItem.setNextRetryAt(OffsetDateTime.now().plusMinutes(1));
		queueItem.setRetryCount(1);

		posSyncQueueRepository.save(queueItem);
	}
}