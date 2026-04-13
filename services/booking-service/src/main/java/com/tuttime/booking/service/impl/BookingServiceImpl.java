package com.tuttime.booking.service.impl;

import com.tuttime.booking.dto.BookingSetupResponse;
import com.tuttime.booking.dto.BookingHistoryResponse;
import com.tuttime.booking.dto.BookingRequest;
import com.tuttime.booking.dto.BookingResponse;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.enums.BookingStatus;
import com.tuttime.booking.event.BookingStatusChangedEvent;
import com.tuttime.booking.grpc.client.MenuGrpcClient;
import com.tuttime.booking.grpc.client.RestaurantGrpcClient;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.MenuItemDetail;
import com.tuttime.common.proto.TableInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final BookingStatusHistoryRepository historyRepository;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final MenuGrpcClient menuGrpcClient;
	private final RedissonClient redissonClient;
	private final ApplicationEventPublisher eventPublisher;

	private static final String LOCK_PREFIX = "lock:table:";

	@Override
	@Transactional
	public BookingSetupResponse getBookingSetupData(UUID branchId, OffsetDateTime date, int partySize) {
		// 1. Fetch external data in parallel
		var tablesFuture = CompletableFuture.supplyAsync(() -> restaurantGrpcClient.getTablesForBooking(branchId, partySize));
		var menuFuture = CompletableFuture.supplyAsync(() -> menuGrpcClient.getPreOrderMenu(branchId));

		// 2. Fetch local occupied tables from SQL
		// status NOT IN ('CANCELED', 'NO_SHOW')
		List<Booking> occupied = bookingRepository.findAllByBranchIdAndBookingDatetime(branchId, date);

		// 3. Combine and filter
		List<TableInfo> physicalTables = tablesFuture.join();
		List<MenuItemDetail> menuItems = menuFuture.join();

		List<BookingSetupResponse.AvailableSlot> slots = calculateSlots(date, physicalTables, occupied);

		return new BookingSetupResponse(
				branchId,
				slots,
				menuItems.stream().map(this::mapToDto).toList()
		);
	}

	@Override
	@Transactional
	public Booking createBooking(BookingRequest request) {
		restaurantGrpcClient.checkAvailableTableForBooking(request);

		// Calculate the end time based on requested duration
		OffsetDateTime startTime = request.bookingDatetime();
		OffsetDateTime endTime = startTime.plus(Duration.ofMinutes(request.duration()));

		// 1. Distributed Locking (Redis)
		// Lock key includes table ID and start time to prevent concurrent requests for the same slot
		String lockKey = LOCK_PREFIX + request.tableId() + ":" + startTime;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// Try to acquire lock for 5s, release after 10s if process hangs
			if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				throw new ConflictException("This table is currently being reserved by another user.");
			}

			// 2. Overlap Check (Postgres Native Logic)
			// Checks if any existing 'active' booking overlaps with the requested [startTime, endTime]
			boolean hasOverlap = bookingRepository.existsOverlappingBooking(
					request.tableId(),
					startTime,
					endTime
			);

			if (hasOverlap) {
				throw new ConflictException("Table is already booked for the selected time range.");
			}

			// 4. Calculate Total Amount (Pre-order food + Fees)
			BigDecimal totalAmount = calculateTotal(request.preOrderItems());

			// 3. Entity Creation (Using Builder pattern)
			Booking booking = Booking.builder()
					.userId(request.userId())
					.restaurantId(request.restaurantId())
					.branchId(request.branchId())
					.zoneId(request.zoneId())
					.tableId(request.tableId())
					.bookingDatetime(startTime)
					.duration(request.duration())
					.partySize(request.partySize())
					.leadTimeMinutes(request.leadTimeMinutes())
					.gracePeriodMins(request.gracePeriodMins())
					.status(BookingStatus.CREATED)
					.totalAmount(totalAmount)
					.autoCancelled(false)
					.build();

			Booking saved = bookingRepository.save(booking);

			// 1. PLACE HERE for initial creation
			eventPublisher.publishEvent(new BookingStatusChangedEvent(
					saved.getId(),
					null,
					BookingStatus.CREATED,
					request.userId(),
					"Initial creation"
			));

			return saved;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException("System interrupted during booking process.");
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Override
	@Transactional
	public Booking updateStatus(UUID bookingId, BookingStatus newStatus, UUID actorId, String reason) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

		BookingStatus oldStatus = booking.getStatus();
		booking.setStatus(newStatus);

		// Save automatically updates 'updated_at' via BaseUuidEntity listeners
		bookingRepository.save(booking);

		// 2. PLACE HERE for status updates
		eventPublisher.publishEvent(new BookingStatusChangedEvent(
				bookingId,
				oldStatus,
				newStatus,
				actorId,
				reason
		));

		return booking;
	}

	@Override
	@Transactional
	public void confirmPayment(UUID bookingId) {
		updateStatus(bookingId, BookingStatus.PAID, null, "Payment successfully processed");
	}

	@Override
	@Transactional
	public void cancelUnpaidBooking(UUID bookingId) {
		updateStatus(bookingId, BookingStatus.CANCELED, null, "Cancelled due to payment timeout");
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingResponse> getBookingsByUser(UUID userId) {
		return bookingRepository.findAllByUserIdOrderByBookingDatetimeDesc(userId)
				.stream()
				.map(BookingResponse::fromEntity)
				.toList();
	}

	@Override
	@Transactional
	public void cancelBooking(UUID bookingId, UUID userId, String reason) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

		// 1. Security Check: Ensure the user owns the booking
		if (!booking.getUserId().equals(userId)) {
			throw new AccessDeniedException("You are not authorized to cancel this booking");
		}

		// 2. Business Rule: Only allow cancellation if not already COMPLETED or CANCELED
		if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELED) {
			throw new ResourceNotFoundException("Booking cannot be cancelled in its current state");
		}

		BookingStatus oldStatus = booking.getStatus();
		booking.setStatus(BookingStatus.CANCELED);
		booking.setCancellationReason(reason != null ? reason : "Cancelled by user");

		bookingRepository.save(booking);

		// 3. Trigger the Audit and Waitlist Notification logic automatically
		eventPublisher.publishEvent(new BookingStatusChangedEvent(
				bookingId, oldStatus, BookingStatus.CANCELED, userId, reason
		));
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingHistoryResponse> getStatusHistory(UUID bookingId) {
		// 1. Verify booking exists
		if (!bookingRepository.existsById(bookingId)) {
			throw new ResourceNotFoundException("Booking not found");
		}

		// 2. Fetch all history entries for this specific booking
		return historyRepository.findAllByBookingIdOrderByChangedAtDesc(bookingId)
				.stream()
				.map(BookingHistoryResponse::fromEntity)
				.toList();
	}

	public List<BookingResponse> getBookingsByRestaurant(UUID restaurantId, OffsetDateTime date) {
		// 1. Define the start and end of the day for the filter
		OffsetDateTime startOfDay = date.truncatedTo(ChronoUnit.DAYS);
		OffsetDateTime endOfDay = startOfDay.plusDays(1);

		// 2. Fetch from repository using your SQL index
		List<Booking> bookings = bookingRepository.findByRestaurantIdAndBookingDatetimeBetween(
				restaurantId,
				startOfDay,
				endOfDay
		);

		// 3. Map to DTOs
		return bookings.stream()
				.map(BookingResponse::fromEntity)
				.toList();
	}

	private List<BookingSetupResponse.AvailableSlot> calculateSlots(OffsetDateTime date, List<TableInfo> physical, List<Booking> occupied) {
		// 1. Define the time intervals for the day (e.g., 18:00 to 22:00, every 30 mins)
		List<OffsetDateTime> timeSlots = generateTimeSlots(date);
		List<BookingSetupResponse.AvailableSlot> result = new ArrayList<>();

		for (OffsetDateTime slotTime : timeSlots) {
			// 2. Find IDs of tables already booked at THIS specific time
			Set<UUID> bookedTableIds = occupied.stream()
					.filter(b -> b.getBookingDatetime().equals(slotTime))
					.map(Booking::getTableId)
					.collect(Collectors.toSet());

			// 3. Filter physical tables: Only keep those NOT in the bookedTableIds set
			List<BookingSetupResponse.TableInfoDto> freeTables = physical.stream()
					.filter(pt -> !bookedTableIds.contains(UUID.fromString(pt.getTableId())))
					.map(this::mapTableToDto) // Uses the mapper defined above
					.toList();

			result.add(new BookingSetupResponse.AvailableSlot(slotTime, freeTables));
		}
		return result;
	}

	private List<OffsetDateTime> generateTimeSlots(OffsetDateTime referenceDateTime) {
		List<OffsetDateTime> slots = new ArrayList<>();

		// Extract the date from the input and set the starting time
		// This ensures we generate slots for the correct day at 12:00
		OffsetDateTime currentSlot = referenceDateTime
				.withHour(12)
				.withMinute(0)
				.withSecond(0)
				.withNano(0);

		// Set the end boundary for the same day at 22:00
		OffsetDateTime endSlot = currentSlot.withHour(22);

		while (currentSlot.isBefore(endSlot)) {
			slots.add(currentSlot);
			currentSlot = currentSlot.plusMinutes(30);
		}

		return slots;
	}

	private BigDecimal calculateTotal(List<BookingRequest.PreOrderItemRequest> items) {
		// Here you would call Menu Service to get fresh prices
		// and sum them up to prevent client-side price manipulation.
		return items.stream()
				.map(item -> item.price().multiply(new BigDecimal(item.quantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BookingSetupResponse.MenuItemDto mapToDto(MenuItemDetail grpcItem) {
		return new BookingSetupResponse.MenuItemDto(
				UUID.fromString(grpcItem.getMenuItemId()),
				grpcItem.getName(),
				grpcItem.getBasePrice(),
				grpcItem.getCategory()
		);
	}

	private BookingSetupResponse.TableInfoDto mapTableToDto(TableInfo grpcTable) {
		return new BookingSetupResponse.TableInfoDto(
				UUID.fromString(grpcTable.getTableId()),
				grpcTable.getTableNumber(),
				grpcTable.getZoneName(),
				grpcTable.getMaxCapacity()
		);
	}

}
