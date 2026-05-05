package com.tuttime.booking.scheduler;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.booking.grpc.client.NotificationGrpcClient;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.booking.entity.Booking;
import com.tuttime.common.event.proto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

	private final BookingRepository bookingRepository;
	private final BookingStatusHistoryRepository historyRepository;
	private final NotificationGrpcClient notificationGrpcClient;
	private final RedissonClient redissonClient;

	/**
	 * Runs every 60 seconds. TODO
	 * Finds bookings created > 1440 mins ago that are still in 'CREATED' status and cancels them.
	 */
	@Scheduled(fixedRate = 60000)
	@Transactional
	public void cleanupUnpaidBookings() {
		OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(1440);
		String reason = "System automatic cancellation: Unpaid after 24 hours";
		String systemUser = "SYSTEM";

		// 1. Fetch bookings that are about to be canceled
		List<Booking> expiredBookings = bookingRepository.findAllByStatusAndCreatedAtBefore(
				BookingStatus.CREATED,
				cutoff
		);

		if (expiredBookings.isEmpty()) {
			return;
		}

		// 2. Update statuses and record history
		expiredBookings.forEach(booking -> {
			BookingStatus oldStatus = booking.getStatus();
			booking.setStatus(BookingStatus.CANCELED);
			booking.setCancellationReason(reason);

			// Save history entry for each
			historyRepository.save(BookingStatusHistory.of(
					booking,
					oldStatus,
					null,
					reason
			));
		});

		// 3. Save all changes (JPA handles the updates automatically via Dirty Checking)
		log.info("Cleanup task: Canceled {} expired unpaid bookings.", expiredBookings.size());
	}

	/**
	 * Runs every minute.
	 * Finds bookings starting in exactly 2 hours.
	 */
	@Scheduled(cron = "0 * * * * *")
	public void scheduleReminders() {
		// 1. Calculate the 1-minute window exactly 2 hours from now
		OffsetDateTime nowPlusTwoHours = OffsetDateTime.now().plusHours(2);
		OffsetDateTime windowStart = nowPlusTwoHours.truncatedTo(ChronoUnit.MINUTES);
		OffsetDateTime windowEnd = windowStart.plusSeconds(59).plusNanos(999_999_999);

		// 2. Fetch eligible bookings
		List<Booking> toRemind = bookingRepository
				.findByBookingDatetimeBetweenAndStatusAndReminderSendFalse(
						windowStart, windowEnd, BookingStatus.CREATED
				);

		for (Booking booking : toRemind) {
			try {
				// 1. Prepare parameters
				Map<String, Object> map = Map.of(
						"bookingId", booking.getId().toString(),
						"branchId", booking.getBranchId(),
						"time", booking.getBookingDatetime().toString(),
						"partySize", booking.getPartySize().toString()
				);

				// 2. Build the Struct
				Struct.Builder structBuilder = Struct.newBuilder();
				map.forEach((k, v) -> structBuilder.putFields(k,
						Value.newBuilder().setStringValue(v.toString()).build()));

				// 3. Call Notification Service
				NotificationRequest request = NotificationRequest.newBuilder()
						.setUserId(booking.getUserId().toString())
						.setTemplateKey(NotificationTemplateKey.BOOKING_REMINDER.name())
						.setParams(structBuilder.build())
						.build();

				notificationGrpcClient.sendNotification(request);

				// 4. Mark as sent to prevent duplicates on retry/restart
				booking.setReminderSend(true);

			} catch (Exception e) {
				log.error("Failed to send reminder for booking {}: {}", booking.getId(), e.getMessage());
			}
		}

		if (!toRemind.isEmpty()) {
			bookingRepository.saveAll(toRemind);
			log.info("Processed {} booking reminders.", toRemind.size());
		}
	}

	@Scheduled(cron = "0 0 * * * *") // Every hour
	public void syncOccupancyCounters() {
		// 1. Define what counts as "Occupied" based on your Enum
		List<BookingStatus> activeStatuses = List.of(
				BookingStatus.CREATED,
				BookingStatus.PAID,
				BookingStatus.ARRIVED
		);

		List<UUID> activeBranchIds = bookingRepository.findDistinctBranchIdsByStatusIn(activeStatuses);

		for (UUID branchId : activeBranchIds) {
			// 3. Use the same list to count
			long actualCount = bookingRepository.countByBranchIdAndStatusIn(branchId, activeStatuses);

			// 4. Update Redis
			redissonClient.getAtomicLong("branch:occupancy:" + branchId).set(actualCount);
		}
	}

}