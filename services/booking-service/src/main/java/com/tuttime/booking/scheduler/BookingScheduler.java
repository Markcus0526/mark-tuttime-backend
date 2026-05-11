package com.tuttime.booking.scheduler;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.booking.grpc.client.NotificationGrpcClient;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.proto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

	private final BookingRepository bookingRepository;
	private final BookingStatusHistoryRepository historyRepository;
	private final NotificationGrpcClient notificationGrpcClient;
	private final RedissonClient redissonClient;
	private final EventProducer eventProducer;

	private static final String OCCUPANCY_PREFIX = "branch:occupancy:";
	private static final String LOCK_PREFIX = "scheduler:";

	// ==================== NO-SHOW PROCESSING ====================
	@Scheduled(cron = "0 * * * * *")
	@Transactional
	public void processNoShows() {
		String lockKey = LOCK_PREFIX + "processNoShows";
		RLock lock = redissonClient.getLock(lockKey);
		try {
			if (!lock.tryLock(0, 55, TimeUnit.SECONDS)) {
				log.debug("Previous no-show process still running, skipping");
				return;
			}
			OffsetDateTime now = OffsetDateTime.now();
			List<Booking> expiredBookings = bookingRepository.findExpiredBookings(now);
			if (expiredBookings.isEmpty()) {
				return;
			}
			for (Booking booking : expiredBookings) {
				try {
					log.info("Marking booking {} as NO_SHOW (Reserved: {})", booking.getId(), booking.getBookingDatetime());
					BookingStatus oldStatus = booking.getStatus();
					booking.setStatus(BookingStatus.NO_SHOW);
					// booking.setAutoCanceled(true); // optional, if field exists
					bookingRepository.save(booking);
					historyRepository.save(BookingStatusHistory.of(
							booking, oldStatus, null, "System auto-canceled due to guest no-show"
					));
					// Decrement occupancy counter if needed (NO_SHOW releases the table)
					redissonClient.getAtomicLong(OCCUPANCY_PREFIX + booking.getBranchId()).decrementAndGet();

					BookingEvent.BookingStatusUpdatedEvent event = BookingEvent.BookingStatusUpdatedEvent.builder()
							.bookingId(booking.getId())
							.tableId(booking.getTableId())
							.branchId(booking.getBranchId())
							.zoneId(booking.getZoneId())
							.userId(booking.getUserId())
							.oldStatus(oldStatus)
							.newStatus(BookingStatus.NO_SHOW)
							.bookingTime(booking.getBookingDatetime())
							.changedBy(null)
							.reason("System auto-canceled due to guest no-show")
							.partySize(booking.getPartySize())
							.build();
					eventProducer.sendBookingStatusUpdatedEvent(event);
				} catch (Exception e) {
					log.error("Failed to process no-show for booking {}", booking.getId(), e);
					// Continue with next booking (if you want to skip one, but transaction will rollback all unless you use separate transactions)
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("No-show processing interrupted");
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	// ==================== UNPAID BOOKINGS CLEANUP ====================
	@Scheduled(cron = "0 */5 * * * *") // every 5 minutes (adjust as needed)
	@Transactional
	public void cleanupUnpaidBookings() {
		String lockKey = LOCK_PREFIX + "cleanupUnpaidBookings";
		RLock lock = redissonClient.getLock(lockKey);
		try {
			if (!lock.tryLock(0, 55, TimeUnit.SECONDS)) {
				log.debug("Previous cleanup task still running, skipping");
				return;
			}
			OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(1440);
			String reason = "System automatic cancellation: Unpaid after 24 hours";

			List<Booking> expiredBookings = bookingRepository.findAllByStatusAndCreatedAtBefore(BookingStatus.CREATED, cutoff);
			if (expiredBookings.isEmpty()) {
				return;
			}

			for (Booking booking : expiredBookings) {
				try {
					BookingStatus oldStatus = booking.getStatus();
					booking.setStatus(BookingStatus.CANCELED);
					booking.setCancellationReason(reason);
					historyRepository.save(BookingStatusHistory.of(booking, oldStatus, null, reason));

					// Decrement occupancy counter
					redissonClient.getAtomicLong(OCCUPANCY_PREFIX + booking.getBranchId()).decrementAndGet();

					BookingEvent.BookingStatusUpdatedEvent event = BookingEvent.BookingStatusUpdatedEvent.builder()
							.bookingId(booking.getId())
							.tableId(booking.getTableId())
							.branchId(booking.getBranchId())
							.userId(booking.getUserId())
							.oldStatus(oldStatus)
							.newStatus(BookingStatus.CANCELED)
							.bookingTime(booking.getBookingDatetime())
							.changedBy(null)
							.reason(reason)
							.build();
					eventProducer.sendBookingStatusUpdatedEvent(event);
				} catch (Exception e) {
					log.error("Failed to cancel expired unpaid booking {}", booking.getId(), e);
				}
			}
			log.info("Cleanup task: Canceled {} expired unpaid bookings.", expiredBookings.size());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Cleanup task interrupted");
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	// ==================== REMINDERS ====================
	@Scheduled(cron = "0 * * * * *")
	@Transactional
	public void scheduleReminders() {
		String lockKey = LOCK_PREFIX + "scheduleReminders";
		RLock lock = redissonClient.getLock(lockKey);
		try {
			if (!lock.tryLock(0, 55, TimeUnit.SECONDS)) {
				log.debug("Previous reminder task still running, skipping");
				return;
			}
			OffsetDateTime nowPlusTwoHours = OffsetDateTime.now().plusHours(2);
			OffsetDateTime windowStart = nowPlusTwoHours.truncatedTo(ChronoUnit.MINUTES);
			OffsetDateTime windowEnd = windowStart.plusSeconds(59).plusNanos(999_999_999);

			List<Booking> toRemind = bookingRepository
					.findByBookingDatetimeBetweenAndStatusAndReminderSendFalse(
							windowStart, windowEnd, BookingStatus.CREATED);

			for (Booking booking : toRemind) {
				try {
					Map<String, Object> params = Map.of(
							"bookingId", booking.getId().toString(),
							"branchId", booking.getBranchId().toString(),
							"time", booking.getBookingDatetime().toString(),
							"partySize", booking.getPartySize().toString()
					);
					Struct.Builder structBuilder = Struct.newBuilder();
					params.forEach((k, v) -> structBuilder.putFields(k,
							Value.newBuilder().setStringValue(v.toString()).build()));

					NotificationRequest request = NotificationRequest.newBuilder()
							.setUserId(booking.getUserId().toString())
							.setTemplateKey(NotificationTemplateKey.BOOKING_REMINDER.name())
							.setParams(structBuilder.build())
							.build();

					notificationGrpcClient.sendNotification(request);
					booking.setReminderSend(true);
				} catch (Exception e) {
					log.error("Failed to send reminder for booking {}: {}", booking.getId(), e.getMessage());
				}
			}
			if (!toRemind.isEmpty()) {
				bookingRepository.saveAll(toRemind);
				log.info("Processed {} booking reminders.", toRemind.size());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Reminder task interrupted");
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	// ==================== OCCUPANCY SYNC ====================
	@Scheduled(cron = "0 0 * * * *") // every hour
	public void syncOccupancyCounters() {
		String lockKey = LOCK_PREFIX + "syncOccupancyCounters";
		RLock lock = redissonClient.getLock(lockKey);
		try {
			if (!lock.tryLock(0, 55, TimeUnit.SECONDS)) {
				log.debug("Previous occupancy sync still running, skipping");
				return;
			}
			// Single query to get counts per branch
			List<Object[]> counts = bookingRepository.countActiveBookingsByBranch();
			for (Object[] row : counts) {
				UUID branchId = (UUID) row[0];
				Long count = (Long) row[1];
				redissonClient.getAtomicLong(OCCUPANCY_PREFIX + branchId).set(count);
			}
			log.debug("Occupancy counters synchronized for {} branches", counts.size());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Occupancy sync interrupted");
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
}