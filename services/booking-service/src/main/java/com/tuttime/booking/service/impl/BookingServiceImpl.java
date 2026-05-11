package com.tuttime.booking.service.impl;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.dto.BookingHistoryDto;
import com.tuttime.booking.dto.BookingSetupDto;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingItem;
import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.booking.grpc.client.MenuGrpcClient;
import com.tuttime.booking.grpc.client.OrderGrpcClient;
import com.tuttime.booking.grpc.client.RestaurantGrpcClient;
import com.tuttime.booking.grpc.client.UserGrpcClient;
import com.tuttime.booking.mapper.BookingHistoryMapper;
import com.tuttime.booking.mapper.BookingMapper;
import com.tuttime.booking.repository.BookingItemRepository;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import com.tuttime.booking.service.BookingService;
import com.tuttime.booking.service.WaitlistService;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.util.NumberUtil;
import com.tuttime.common.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tuttime.common.util.UuidUtil.NIL_UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final BookingItemRepository bookingItemRepository;
	private final BookingStatusHistoryRepository historyRepository;
	private final BookingMapper bookingMapper;
	private final BookingHistoryMapper bookingHistoryMapper;
	private final WaitlistService waitlistService;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final UserGrpcClient userGrpcClient;
	private final MenuGrpcClient menuGrpcClient;
	private final OrderGrpcClient orderGrpcClient;
	private final RedissonClient redissonClient;
	private final EventProducer eventProducer;

	@Value("${app.booking.slot-interval}")
	private int slotInterval;

	@Value("${app.booking.dining-duration}")
	private int diningDuration;

	private static final String LOCK_PREFIX = "lock:table:";

	@Override
	@Transactional
	public BookingDto.BookingResponse createBooking(BookingDto.BookingRequest request, UUID currentUserId, String userRoles) {
		TableDetailResponse response = restaurantGrpcClient.getTableDetail(request.tableId());
		if (!response.getAvailable()) {
			throw new InvalidRequestException("Table is no longer available.");
		}

		UUID tableId = UuidUtil.toUuid(response.getTableId());
		// FIXED: consistent lock key (without timestamp)
		RLock lock = redissonClient.getLock(LOCK_PREFIX + tableId);

		try {
			if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				throw new ConflictException("Table is being reserved by another user.");
			}

			OffsetDateTime startTime = request.bookingDatetime();
			OffsetDateTime endTime = startTime.plus(Duration.ofMinutes(request.duration()));
			if (bookingRepository.existsOverlappingBooking(tableId, startTime, endTime)) {
				throw new ConflictException("Table is already booked for this time range.");
			}

			List<MenuItemDetailResponse> menuDetails = menuGrpcClient.getMenuDetails(
					request.preOrderItems().stream().map(i -> i.menuItemId().toString()).toList()
			);
			validateModifierSelections(request.preOrderItems(), menuDetails);
			BigDecimal totalAmount = calculateFinalPrice(request.preOrderItems(), menuDetails);

			Booking booking = Booking.builder()
					.userId(request.userId())
					.restaurantId(UuidUtil.toUuid(response.getRestaurantId()))
					.branchId(UuidUtil.toUuid(response.getBranchId()))
					.zoneId(UuidUtil.toUuid(response.getZoneId()))
					.tableId(tableId)
					.bookingDatetime(startTime)
					.duration(request.duration())
					.partySize(request.partySize())
					.leadTimeMinutes(request.leadTimeMinutes())
					.gracePeriodMins(request.gracePeriodMins())
					.status(BookingStatus.CREATED)
					.totalAmount(totalAmount)
					.guestName(request.guestName())
					.guestPhone(request.guestPhone())
					.specialRequest(request.specialRequest())
					.build();

			Booking savedBooking = bookingRepository.save(booking);

			// occupancy logic
			String occupancyKey = "branch:occupancy:" + savedBooking.getBranchId();
			RAtomicLong activeBookingsCounter = redissonClient.getAtomicLong(occupancyKey);
			long currentCount = activeBookingsCounter.incrementAndGet();

			int totalTables = response.getTotalTables();
			double occupancy = ((double) currentCount / totalTables) * 100;
			if (occupancy >= 90.0) {
				BookingEvent.BranchAvailabilityEvent availabilityEvent = BookingEvent.BranchAvailabilityEvent.builder()
						.eventId(UUID.randomUUID())
						.branchId(response.getBranchId())
						.branchName(response.getBranchName())
						.occupancyPercent(occupancy)
						.totalTables(totalTables)
						.bookedTables((int) currentCount)
						.build();
				eventProducer.sendBookingBranchAvailabilityEvent(availabilityEvent);
			}

			saveBookingItems(savedBooking, request.preOrderItems(), menuDetails);
			historyRepository.save(BookingStatusHistory.of(savedBooking, null, request.userId(), "Initial creation"));

			BookingEvent.BookingStatusUpdatedEvent event = BookingEvent.BookingStatusUpdatedEvent.builder()
					.bookingId(savedBooking.getId())
					.tableId(savedBooking.getTableId())
					.branchId(savedBooking.getBranchId())
					.userId(savedBooking.getUserId())
					.oldStatus(null)
					.newStatus(BookingStatus.CREATED)
					.bookingTime(savedBooking.getBookingDatetime())
					.changedBy(currentUserId)
					.reason("Created")
					.build();
			eventProducer.sendBookingStatusUpdatedEvent(event);

			// Create order
			CreateOrderRequest.Builder orderRequestBuilder = CreateOrderRequest.newBuilder()
					.setBookingId(savedBooking.getId().toString())
					.setUserId(savedBooking.getUserId().toString())
					.setBranchId(savedBooking.getBranchId().toString())
					.setTableId(savedBooking.getTableId().toString())
					.setGuestName(savedBooking.getGuestName())
					.setGuestPhone(savedBooking.getGuestPhone())
					.setSpecialRequest(savedBooking.getSpecialRequest());

			request.preOrderItems().forEach(itemDto -> {
				OrderItemRequest.Builder itemEntry = OrderItemRequest.newBuilder()
						.setMenuItemId(itemDto.menuItemId().toString())
						.setQuantity(itemDto.quantity());

				BigDecimal verifiedBasePrice = menuDetails.stream()
						.filter(m -> m.getMenuItemId().equals(itemDto.menuItemId().toString()))
						.findFirst()
						.map(m -> NumberUtil.stringToBigDecimal(m.getBasePrice()))
						.orElse(BigDecimal.ZERO);
				itemEntry.setPrice(verifiedBasePrice.toString());

				if (itemDto.modifierIds() != null) {
					itemDto.modifierIds().forEach(modId -> {
						BigDecimal adjustment = menuDetails.stream()
								.flatMap(m -> m.getModifierGroupsList().stream())
								.flatMap(group -> group.getOptionsList().stream())
								.filter(option -> option.getOptionId().equals(modId.toString()))
								.findFirst()
								.map(option -> NumberUtil.stringToBigDecimal(option.getExtraPrice()))
								.orElse(BigDecimal.ZERO);
						itemEntry.addModifiers(ModifierRequest.newBuilder()
								.setModifierId(modId.toString())
								.setPriceAdjustment(adjustment.toString())
								.build());
					});
				}
				orderRequestBuilder.addItems(itemEntry.build());
			});

			CreateOrderResponse orderResponse = orderGrpcClient.createOrder(orderRequestBuilder.build());
			savedBooking.setOrderId(UuidUtil.toUuid(orderResponse.getOrderId()));
			savedBooking = bookingRepository.save(savedBooking);

			return bookingMapper.toResponse(savedBooking);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException("System interrupted.");
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Override
	@Transactional
	public BookingDto.BookingResponse editBooking(UUID bookingId,
	                                              BookingDto.BookingUpdateRequest request,
	                                              UUID currentUserId,
	                                              String userRoles) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new InvalidRequestException("Booking not found"));

		if (booking.getStatus() != BookingStatus.CREATED && booking.getStatus() != BookingStatus.PAID) {
			throw new InvalidRequestException("Cannot edit booking in status: " + booking.getStatus());
		}

		boolean slotChanged = false;
		boolean itemsChanged = false;
		UUID newTableId = null;
		OffsetDateTime newStartTime = null;
		OffsetDateTime newEndTime = null;

		// --- Handle time/table change if provided ---
		if (request.bookingDatetime() != null || request.tableId() != null) {
			newStartTime = request.bookingDatetime() != null ? request.bookingDatetime() : booking.getBookingDatetime();
			newEndTime = newStartTime.plusMinutes(booking.getDuration());
			newTableId = request.tableId() != null ? request.tableId() : booking.getTableId();
			slotChanged = !booking.getBookingDatetime().equals(newStartTime) || !booking.getTableId().equals(newTableId);
		}

		if (slotChanged) {
			RLock newLock = redissonClient.getLock(LOCK_PREFIX + newTableId);
			try {
				if (!newLock.tryLock(5, 10, TimeUnit.SECONDS)) {
					throw new ConflictException("The new table/slot is currently being reserved by someone else.");
				}
				if (bookingRepository.existsOverlappingBooking(newTableId, newStartTime, newEndTime)) {
					newLock.unlock();
					throw new ConflictException("Table is already booked for this new time range.");
				}
				// Do NOT unlock the old lock – it will expire automatically.
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new BusinessException("System interrupted during re-locking.");
			}
		}

		// --- Handle items update if provided ---
		List<MenuItemDetailResponse> menuDetails = null;
		BigDecimal newTotal = null;
		if (request.preOrderItems() != null) {
			menuDetails = menuGrpcClient.getMenuDetails(
					request.preOrderItems().stream().map(i -> i.menuItemId().toString()).toList()
			);
			validateModifierSelections(request.preOrderItems(), menuDetails);
			newTotal = calculateFinalPrice(request.preOrderItems(), menuDetails);
			itemsChanged = true;
		}

		// --- Update fields only if present ---
		if (request.guestName() != null) booking.setGuestName(request.guestName());
		if (request.guestPhone() != null) booking.setGuestPhone(request.guestPhone());
		if (request.specialRequest() != null) booking.setSpecialRequest(request.specialRequest());
		if (request.partySize() != null) booking.setPartySize(request.partySize());
		if (slotChanged) {
			booking.setTableId(newTableId);
			booking.setBookingDatetime(newStartTime);
		}
		if (itemsChanged) {
			// Replace existing items
			booking.getItems().clear();
			saveBookingItems(booking, request.preOrderItems(), menuDetails);
			booking.setTotalAmount(newTotal);
		}

		if (slotChanged && booking.getOrderId() != null) {
			UpdateOrderRequest orderUpdateRequest = UpdateOrderRequest.newBuilder()
					.setOrderId(booking.getOrderId().toString())
					.setNewBookingTime(booking.getBookingDatetime().toString())
					.setNewTotal(booking.getTotalAmount().toString())
					.setGuestName(booking.getGuestName())
					.setGuestPhone(booking.getGuestPhone())
					.build();
			UpdateOrderResponse orderResponse = orderGrpcClient.updateOrder(orderUpdateRequest);
			if (!orderResponse.getSuccess())
				throw new InvalidRequestException("Cannot update order for this booking.");
		}

		booking.setUpdatedAt(OffsetDateTime.now());
		Booking updatedBooking = bookingRepository.save(booking);
		historyRepository.save(BookingStatusHistory.of(updatedBooking, updatedBooking.getStatus(), currentUserId, "Booking Edited"));

		return bookingMapper.toResponse(updatedBooking);
	}

	@Override
	@Transactional(readOnly = true)
	public BookingSetupDto.BookingSetupResponse getAvailableSlots(UUID branchId, OffsetDateTime date, int partySize, UUID currentUserId) {
		// ... (unchanged, but already fine) ...
		LocalDate requestedDate = date.toLocalDate();
		ZoneOffset offset = date.getOffset();
		OffsetDateTime now = OffsetDateTime.now(offset);
		if (requestedDate.isBefore(now.toLocalDate())) {
			return new BookingSetupDto.BookingSetupResponse(branchId, Collections.emptyList());
		}
		var tablesFuture = CompletableFuture.supplyAsync(() -> restaurantGrpcClient.getBranchSlots(branchId, partySize));
		var scheduleFuture = CompletableFuture.supplyAsync(() -> restaurantGrpcClient.getBranchSchedule(branchId, requestedDate.getDayOfWeek().getValue()));

		OffsetDateTime startOfSearch = requestedDate.atStartOfDay().atOffset(offset);
		OffsetDateTime endOfSearch = startOfSearch.plusDays(1);
		List<Booking> occupied = bookingRepository.findAllByBranchIdAndDayRange(branchId, startOfSearch, endOfSearch);

		List<TableSlotResponse> physicalTables = tablesFuture.join();
		ScheduleResponse schedule = scheduleFuture.join();
		if (schedule.getClosed()) {
			return new BookingSetupDto.BookingSetupResponse(branchId, Collections.emptyList());
		}

		Map<UUID, List<Booking>> bookingsByTable = occupied.stream()
				.collect(Collectors.groupingBy(Booking::getTableId));

		OffsetDateTime openTime = requestedDate.atTime(LocalTime.parse(schedule.getOpenTime())).atOffset(offset);
		OffsetDateTime closeTime = requestedDate.atTime(LocalTime.parse(schedule.getCloseTime())).atOffset(offset);
		OffsetDateTime currentSlot = requestedDate.equals(now.toLocalDate())
				? roundToNextInterval(now.plusMinutes(15), slotInterval)
				: openTime;
		if (currentSlot.isBefore(openTime)) currentSlot = openTime;

		List<BookingSetupDto.BookingSetupResponse.AvailableSlot> result = new ArrayList<>();
		while (currentSlot.plusMinutes(diningDuration).isBefore(closeTime) ||
				currentSlot.plusMinutes(diningDuration).isEqual(closeTime)) {
			final OffsetDateTime start = currentSlot;
			final OffsetDateTime end = currentSlot.plusMinutes(diningDuration);
			List<TableSlotResponse> freeTables = physicalTables.stream()
					.filter(pt -> {
						List<Booking> list = bookingsByTable.get(UUID.fromString(pt.getTableId()));
						return list == null || list.stream().noneMatch(b ->
								start.isBefore(b.getBookingDatetime().plusMinutes(b.getDuration())) &&
										end.isAfter(b.getBookingDatetime()));
					})
					.toList();
			result.add(new BookingSetupDto.BookingSetupResponse.AvailableSlot(
					start,
					!freeTables.isEmpty(),
					freeTables.size(),
					freeTables.stream().map(TableSlotResponse::getZoneName).distinct().toList()
			));
			currentSlot = currentSlot.plusMinutes(slotInterval);
		}
		return new BookingSetupDto.BookingSetupResponse(branchId, result);
	}

	@Override
	@Transactional
	public BookingDto.BookingResponse updateStatus(UUID bookingId, BookingStatus newStatus, String reason, UUID currentUserId, String userRoles) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new InvalidRequestException("Booking not found: " + bookingId));
		restaurantGrpcClient.validateBranchPermission(booking.getBranchId(), currentUserId, userRoles);
		BookingStatus oldStatus = booking.getStatus();
		if (oldStatus == BookingStatus.CANCELED || oldStatus == BookingStatus.COMPLETED) {
			throw new InvalidRequestException("Cannot change status of a finished or canceled booking.");
		}
		booking.setStatus(newStatus);
		if (newStatus == BookingStatus.CANCELED) {
			booking.setCancellationReason(reason);
		}
		bookingRepository.save(booking);
		String occupancyKey = "branch:occupancy:" + booking.getBranchId();
		RAtomicLong activeBookingsCounter = redissonClient.getAtomicLong(occupancyKey);
		if (isActive(oldStatus) && !isActive(newStatus)) {
			activeBookingsCounter.decrementAndGet();
		} else if (!isActive(oldStatus) && isActive(newStatus)) {
			long currentCount = activeBookingsCounter.incrementAndGet();
			checkAndNotifyLowAvailability(booking, currentCount);
		}
		historyRepository.save(BookingStatusHistory.of(booking, oldStatus, currentUserId, reason));
		BookingEvent.BookingStatusUpdatedEvent event = BookingEvent.BookingStatusUpdatedEvent.builder()
				.bookingId(booking.getId())
				.tableId(booking.getTableId())
				.branchId(booking.getBranchId())
				.userId(booking.getUserId())
				.oldStatus(oldStatus)
				.newStatus(newStatus)
				.bookingTime(booking.getBookingDatetime())
				.changedBy(currentUserId)
				.reason(reason)
				.build();
		eventProducer.sendBookingStatusUpdatedEvent(event);
		return bookingMapper.toResponse(booking);
	}

	@Override
	@Transactional(readOnly = true)
	public BookingDto.BookingDetailResponse getBooking(UUID bookingId, UUID currentUserId, String userRoles) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new InvalidRequestException("Booking not found"));
		restaurantGrpcClient.validateBranchPermission(booking.getBranchId(), currentUserId, userRoles);
		return getEnrichedBookings(List.of(booking)).getFirst();
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingDto.BookingDetailResponse> getBookingsByUser(UUID userId) {
		List<Booking> bookings = bookingRepository.findAllByUserIdOrderByBookingDatetimeDesc(userId);
		if (bookings.isEmpty()) return List.of();
		return getEnrichedBookings(bookings);
	}

	@Override
	@Transactional
	public void deleteBooking(UUID bookingId, UUID userId, String reason, UUID currentUserId, String userRoles) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new InvalidRequestException("Booking not found"));
		if (!booking.getUserId().equals(userId)) {
			throw new ForbiddenException("You are not authorized to cancel this booking");
		}
		if (booking.getStatus() == BookingStatus.COMPLETED ||
				booking.getStatus() == BookingStatus.CANCELED ||
				booking.getStatus() == BookingStatus.ARRIVED) {
			throw new InvalidRequestException("Booking cannot be canceled in its current state: " + booking.getStatus());
		}
		BookingStatus oldStatus = booking.getStatus();
		if (oldStatus == BookingStatus.PAID && booking.getOrderId() != null && booking.getPaymentId() != null) {
			CancelOrderRequest cancelRequest = CancelOrderRequest.newBuilder()
					.setBookingId(booking.getId().toString())
					.setOrderId(booking.getOrderId().toString())
					.setPaymentId(booking.getPaymentId().toString())
					.setReason(reason != null ? reason : "Canceled by user")
					.build();
			CancelOrderResponse orderResponse = orderGrpcClient.cancelAndRefund(cancelRequest);
			if (!orderResponse.getSuccess()) {
				log.warn("Booking {} Refund failed. Current status: {}", bookingId, oldStatus);
				throw new InvalidRequestException("Refund failed: " + orderResponse.getReason());
			}
		}
		booking.setStatus(BookingStatus.CANCELED);
		booking.setCancellationReason(reason != null ? reason : "Canceled by user");
		bookingRepository.save(booking);
		historyRepository.save(BookingStatusHistory.of(booking, oldStatus, currentUserId, reason));
		// FIXED: Use same lock key without timestamp
		RLock lock = redissonClient.getLock(LOCK_PREFIX + booking.getTableId());
		if (lock.isHeldByCurrentThread()) {
			lock.unlock();
		}
		BookingEvent.BookingStatusUpdatedEvent event = BookingEvent.BookingStatusUpdatedEvent.builder()
				.bookingId(booking.getId())
				.tableId(booking.getTableId())
				.branchId(booking.getBranchId())
				.zoneId(booking.getZoneId())
				.userId(booking.getUserId())
				.oldStatus(oldStatus)
				.newStatus(BookingStatus.CANCELED)
				.bookingTime(booking.getBookingDatetime())
				.changedBy(currentUserId)
				.reason(reason)
				.partySize(booking.getPartySize())
				.build();
		eventProducer.sendBookingStatusUpdatedEvent(event);

		waitlistService.notifyNextWaiting(booking.getBranchId(), booking.getZoneId(), booking.getPartySize());
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingHistoryDto.BookingHistoryResponse> getStatusHistory(UUID bookingId) {
		if (!bookingRepository.existsById(bookingId)) {
			throw new InvalidRequestException("Booking not found");
		}
		List<BookingStatusHistory> histories = historyRepository.findAllByBookingIdOrderByChangedAtDesc(bookingId);
		return histories.stream()
				.map(bookingHistoryMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<BookingDto.BookingDetailResponse> getBookingsByBranch(UUID branchId, OffsetDateTime date, UUID currentUserId, String userRoles) {
		OffsetDateTime startOfDay = date.truncatedTo(ChronoUnit.DAYS);
		OffsetDateTime endOfDay = startOfDay.plusDays(1);
		List<Booking> bookings = bookingRepository.findAllByBranchIdAndBookingDatetimeBetween(branchId, startOfDay, endOfDay);
		if (bookings.isEmpty()) return List.of();
		return getEnrichedBookings(bookings);
	}

	@Transactional
	public void markAsPaid(UUID paymentId, UUID bookingId, UUID staffOrSystemId, String paymentRef) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new InvalidRequestException("Booking not found"));
		booking.setPaymentId(paymentId);
		BookingStatus oldStatus = booking.getStatus();
		if (oldStatus != BookingStatus.CREATED) {
			log.warn("Booking {} already processed. Current status: {}", bookingId, oldStatus);
			return;
		}
		booking.setStatus(BookingStatus.PAID);
		bookingRepository.save(booking);
		historyRepository.save(BookingStatusHistory.of(booking, oldStatus, staffOrSystemId, "Payment confirmed. Ref: " + paymentRef));
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				BookingEvent.BookingStatusUpdatedEvent event = BookingEvent.BookingStatusUpdatedEvent.builder()
						.bookingId(booking.getId())
						.tableId(booking.getTableId())
						.branchId(booking.getBranchId())
						.userId(booking.getUserId())
						.oldStatus(oldStatus)
						.newStatus(BookingStatus.PAID)
						.bookingTime(booking.getBookingDatetime())
						.changedBy(staffOrSystemId)
						.reason("Payment confirmed. Ref: " + paymentRef)
						.build();
				eventProducer.sendBookingStatusUpdatedEvent(event);
				// Do NOT unlock – lock is not held by this method.
			}
		});
	}

	@Override
	@Transactional
	public Booking createBookingFromWaitlist(BookingDto.BookingRequest request, UUID currentUserId, String userRoles) {
		// 1. Create the booking using the full createBooking flow (status = CREATED)
		BookingDto.BookingResponse response = createBooking(request, currentUserId, userRoles);

		// 2. Fetch the newly created booking
		Booking booking = bookingRepository.findById(response.bookingId())
				.orElseThrow(() -> new InvalidRequestException("Booking not found after creation"));

		// 3. Change status to CONFIRMED (skip payment)
		BookingStatus oldStatus = booking.getStatus();
		booking.setStatus(BookingStatus.CREATED);
		bookingRepository.save(booking);

		// 4. Record audit history for the status change
		historyRepository.save(BookingStatusHistory.of(booking, oldStatus, currentUserId, "Promoted from waitlist claim"));

		// 5. Publish status update event (optional but recommended)
		BookingEvent.BookingStatusUpdatedEvent event = BookingEvent.BookingStatusUpdatedEvent.builder()
				.bookingId(booking.getId())
				.tableId(booking.getTableId())
				.branchId(booking.getBranchId())
				.userId(booking.getUserId())
				.oldStatus(oldStatus)
				.newStatus(BookingStatus.CREATED)
				.bookingTime(booking.getBookingDatetime())
				.changedBy(currentUserId)
				.reason("Waitlist claim")
				.partySize(booking.getPartySize())
				.build();
		eventProducer.sendBookingStatusUpdatedEvent(event);

		return booking;
	}

	// ---------- Helper methods ----------

	public void validateModifierSelections(List<BookingDto.BookingRequest.PreOrderItemRequest> selections,
	                                       List<MenuItemDetailResponse> menuDetails) {
		for (var selection : selections) {
			MenuItemDetailResponse menu = menuDetails.stream()
					.filter(m -> m.getMenuItemId().equals(selection.menuItemId().toString()))
					.findFirst()
					.orElseThrow(() -> new BusinessException("Menu item not found: " + selection.menuItemId()));
			// Collect allowed modifier IDs
			Set<String> allowedModifierIds = menu.getModifierGroupsList().stream()
					.flatMap(group -> group.getOptionsList().stream())
					.map(ModifierOptionResponse::getOptionId)
					.collect(Collectors.toSet());
			// Reject any modifier not allowed
			if (selection.modifierIds() != null) {
				for (UUID modId : selection.modifierIds()) {
					if (!allowedModifierIds.contains(modId.toString())) {
						throw new InvalidRequestException("Modifier ID " + modId + " is not valid for item " + selection.menuItemId());
					}
				}
			}
			// Min/Max validation
			for (ModifierGroupResponse group : menu.getModifierGroupsList()) {
				long selectedInGroupCount = selection.modifierIds() == null ? 0 :
						selection.modifierIds().stream()
						.filter(id -> group.getOptionsList().stream()
						              .anyMatch(opt -> opt.getOptionId().equals(id.toString())))
						.count();
				if (selectedInGroupCount < group.getMinSelect()) {
					throw new InvalidRequestException(String.format("Selection required for '%s' in '%s'", group.getGroupName(), menu.getName()));
				}
				if (group.getMaxSelect() > 0 && selectedInGroupCount > group.getMaxSelect()) {
					throw new InvalidRequestException(String.format("Too many selections for '%s' in '%s'", group.getGroupName(), menu.getName()));
				}
			}
		}
	}

	private BigDecimal calculateFinalPrice(List<BookingDto.BookingRequest.PreOrderItemRequest> selections,
	                                       List<MenuItemDetailResponse> menuDetails) {
		BigDecimal grandTotal = BigDecimal.ZERO;
		for (var selection : selections) {
			MenuItemDetailResponse menu = menuDetails.stream()
					.filter(m -> m.getMenuItemId().equals(selection.menuItemId().toString()))
					.findFirst()
					.orElseThrow(() -> new BusinessException("Menu item mismatch"));
			BigDecimal itemPrice = NumberUtil.stringToBigDecimal(menu.getBasePrice());
			if (selection.modifierIds() != null) {
				for (UUID modId : selection.modifierIds()) {
					BigDecimal extra = menu.getModifierGroupsList().stream()
							.flatMap(g -> g.getOptionsList().stream())
							.filter(o -> o.getOptionId().equals(modId.toString()))
							.findFirst()
							.map(o -> NumberUtil.stringToBigDecimal(o.getExtraPrice()))
							.orElse(BigDecimal.ZERO);
					itemPrice = itemPrice.add(extra);
				}
			}
			grandTotal = grandTotal.add(itemPrice.multiply(BigDecimal.valueOf(selection.quantity())));
		}
		return grandTotal;
	}

	private void saveBookingItems(Booking savedBooking,
	                              List<BookingDto.BookingRequest.PreOrderItemRequest> preOrderItems,
	                              List<MenuItemDetailResponse> menuDetails) {
		if (preOrderItems == null || preOrderItems.isEmpty()) return;
		List<BookingItem> entities = preOrderItems.stream().map(itemDto -> {
			MenuItemDetailResponse menu = menuDetails.stream()
					.filter(m -> m.getMenuItemId().equals(itemDto.menuItemId().toString()))
					.findFirst()
					.orElseThrow(() -> new BusinessException("Item mismatch"));
			BigDecimal unitPrice = NumberUtil.stringToBigDecimal(menu.getBasePrice());
			if (itemDto.modifierIds() != null) {
				for (UUID modId : itemDto.modifierIds()) {
					BigDecimal extra = menu.getModifierGroupsList().stream()
							.flatMap(g -> g.getOptionsList().stream())
							.filter(o -> o.getOptionId().equals(modId.toString()))
							.findFirst()
							.map(o -> NumberUtil.stringToBigDecimal(o.getExtraPrice()))
							.orElse(BigDecimal.ZERO);
					unitPrice = unitPrice.add(extra);
				}
			}
			return BookingItem.builder()
					.booking(savedBooking)
					.menuItemId(itemDto.menuItemId())
					.quantity(itemDto.quantity())
					.unitPrice(unitPrice)
					.totalPrice(unitPrice.multiply(BigDecimal.valueOf(itemDto.quantity())))
					.notes(itemDto.notes())
					.build();
		}).toList();
		bookingItemRepository.saveAll(entities);
	}

	private List<BookingDto.BookingDetailResponse> getEnrichedBookings(List<Booking> bookings) {
		Set<String> userIds = bookings.stream().map(b -> b.getUserId().toString()).collect(Collectors.toSet());
		Set<String> tableIds = bookings.stream().map(b -> b.getTableId().toString()).collect(Collectors.toSet());
		List<String> menuItemIds = bookings.stream()
				.flatMap(b -> b.getItems().stream().map(i -> i.getMenuItemId().toString()))
				.distinct().toList();

		var usersFuture = CompletableFuture.supplyAsync(() -> fetchUsersMap(userIds));
		var tablesFuture = CompletableFuture.supplyAsync(() -> fetchTablesMap(tableIds));
		var menusFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return menuGrpcClient.getMenuDetails(menuItemIds).stream()
						.collect(Collectors.toMap(MenuItemDetailResponse::getMenuItemId, m -> m, (a, b) -> a));
			} catch (Exception e) {
				log.warn("Menu service failed, proceeding with empty menu data");
				return Collections.<String, MenuItemDetailResponse>emptyMap();
			}
		});

		// FIXED: Add timeout to prevent indefinite blocking
		try {
			CompletableFuture.allOf(usersFuture, tablesFuture, menusFuture)
					.orTimeout(5, TimeUnit.SECONDS)
					.join();
		} catch (Exception e) {
			log.error("Timeout or error while enriching bookings", e);
		}

		var userMap = usersFuture.join();
		var tableMap = tablesFuture.join();
		var menuMap = menusFuture.join();

		return bookings.stream().map(booking -> {
			var user = userMap.getOrDefault(booking.getUserId().toString(),
					UserDetailResponse.newBuilder().setUserId(NIL_UUID).setFullName("Deleted User").build());
			var table = tableMap.getOrDefault(booking.getTableId().toString(),
					TableHierarchyResponse.newBuilder().setTableId(NIL_UUID).setTableLabel("N/A").build());
			var bookingMenus = booking.getItems().stream()
					.map(i -> menuMap.get(i.getMenuItemId().toString()))
					.filter(Objects::nonNull)
					.toList();
			return bookingMapper.toDetailResponse(booking, user, table, bookingMenus);
		}).toList();
	}

	private Map<String, UserDetailResponse> fetchUsersMap(Set<String> ids) {
		return ids.parallelStream().collect(Collectors.toMap(
				id -> id,
				id -> {
					try {
						return userGrpcClient.getUserDetail(UserDetailRequest.newBuilder().setUserId(id).build());
					} catch (Exception e) {
						return UserDetailResponse.newBuilder().setUserId(NIL_UUID).build();
					}
				}
		));
	}

	private Map<String, TableHierarchyResponse> fetchTablesMap(Set<String> ids) {
		return ids.parallelStream().collect(Collectors.toMap(
				id -> id,
				id -> {
					try {
						return restaurantGrpcClient.getTableHierarchyDetail(TableHierarchyRequest.newBuilder().setTableId(id).build());
					} catch (Exception e) {
						return TableHierarchyResponse.newBuilder()
								.setTableId(NIL_UUID)
								.setZoneId(NIL_UUID)
								.setBranchId(NIL_UUID)
								.setRestaurantId(NIL_UUID)
								.setTableLabel("N/A")
								.setZoneName("Unknown")
								.setRestaurantName("Unknown")
								.setBranchName("Unknown")
								.setBranchAddress("N/A")
								.setBranchPhone("N/A")
								.build();
					}
				}
		));
	}

	private void checkAndNotifyLowAvailability(Booking booking, long currentCount) {
		TableDetailResponse tableDetail = restaurantGrpcClient.getTableDetail(booking.getTableId());
		int totalTables = tableDetail.getTotalTables();
		double occupancy = ((double) currentCount / totalTables) * 100;
		if (occupancy >= 90.0) {
			BookingEvent.BranchAvailabilityEvent event = BookingEvent.BranchAvailabilityEvent.builder()
					.eventId(UUID.randomUUID())
					.branchId(booking.getBranchId().toString())
					.branchName(tableDetail.getBranchName())
					.occupancyPercent(occupancy)
					.totalTables(totalTables)
					.bookedTables((int) currentCount)
					.build();
			eventProducer.sendBookingBranchAvailabilityEvent(event);
		}
	}

	private boolean isActive(BookingStatus status) {
		return status == BookingStatus.CREATED || status == BookingStatus.PAID || status == BookingStatus.ARRIVED;
	}

	private OffsetDateTime roundToNextInterval(OffsetDateTime time, int interval) {
		int minutes = time.getMinute();
		int mod = minutes % interval;
		int add = (mod == 0) ? 0 : (interval - mod);
		return time.plusMinutes(add).withSecond(0).withNano(0);
	}
}