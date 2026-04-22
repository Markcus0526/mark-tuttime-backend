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
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.kafka.BookingEvent;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.util.NumberUtil;
import com.tuttime.common.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final UserGrpcClient userGrpcClient;
	private final MenuGrpcClient menuGrpcClient;
	private final OrderGrpcClient orderGrpcClient;
	private final RedissonClient redissonClient;
	private final EventProducer eventProducer;

	private static final String LOCK_PREFIX = "lock:table:";

	@Override
	@Transactional
	public BookingDto.BookingResponse createBooking(BookingDto.BookingRequest request, UUID currentUserId, String userRoles) {
		TableDetailResponse response = restaurantGrpcClient.getTableDetail(request.tableId());
		if (!response.getAvailable()) {
			throw new InvalidRequestException("Table is no longer available.");
		}

		UUID tableId = UuidUtil.toUuid(response.getTableId());
		RLock lock = redissonClient.getLock(LOCK_PREFIX + tableId);

		try {
			if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				throw new ConflictException("Table is being reserved by another user.");
			}

			// 2. Overlap Check
			OffsetDateTime startTime = request.bookingDatetime();
			OffsetDateTime endTime = startTime.plus(Duration.ofMinutes(request.duration()));
			if (bookingRepository.existsOverlappingBooking(tableId, startTime, endTime)) {
				throw new ConflictException("Table is already booked for this time range.");
			}

			// 3. SECURE PRICE & MODIFIER VALIDATION (New Step)
			// Fetch full menu details from gRPC to get base_price and modifier rules
			List<MenuItemDetailResponse> menuDetails = menuGrpcClient.getMenuDetails(
					request.preOrderItems().stream().map(i -> i.menuItemId().toString()).toList()
			);

			// Validate selection limits (Min/Max)
			validateModifierSelections(request.preOrderItems(), menuDetails);

			// Calculate Total safely using Server-Side prices (Base + Extra Options)
			BigDecimal totalAmount = calculateFinalPrice(request.preOrderItems(), menuDetails);

			// 4. Save Booking
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
					.build();

			Booking savedBooking = bookingRepository.save(booking);

			// 5. Save Items with Selected Modifiers
			// Note: You should update your BookingItem entity to store a JSON or link to selected options
			saveBookingItems(savedBooking, request.preOrderItems(), menuDetails);

			// 6. Audit & Events
			historyRepository.save(BookingStatusHistory.of(savedBooking, null, request.userId(), "Initial creation"));
			eventProducer.sendBookingStatusUpdatedEvent(new BookingEvent.BookingStatusUpdatedEvent(
					savedBooking.getId(),
					savedBooking.getTableId(),
					savedBooking.getBranchId(),
					savedBooking.getUserId(),
					null,
					BookingStatus.CREATED,
					savedBooking.getBookingDatetime(),
					currentUserId,
					"Created",
					OffsetDateTime.now()));

			// 7. Initialize Order in Order Service
			CreateOrderRequest.Builder orderRequestBuilder = CreateOrderRequest.newBuilder()
					.setBookingId(savedBooking.getId().toString())
					.setUserId(savedBooking.getUserId().toString())
					.setBranchId(savedBooking.getBranchId().toString())
					.setTableId(savedBooking.getTableId().toString());

			// Loop through the items in your BookingRequest
			request.preOrderItems().forEach(itemDto -> {
				OrderItemRequest.Builder itemEntry = OrderItemRequest.newBuilder()
						.setMenuItemId(itemDto.menuItemId().toString())
						.setQuantity(itemDto.quantity());

				// We must provide the PRICE we validated in Step 3
				// Find the item in our menuDetails list to get the server-verified price
				BigDecimal verifiedBasePrice = menuDetails.stream()
						.filter(m -> m.getMenuItemId().equals(itemDto.menuItemId().toString()))
						.findFirst()
						.map(m -> NumberUtil.stringToBigDecimal(m.getBasePrice())) // Convert String to BigDecimal here
						.orElse(BigDecimal.ZERO);

				itemEntry.setPrice(verifiedBasePrice.toString());

				// Map Modifiers and their adjustments
				if (itemDto.modifierIds() != null) {
					itemDto.modifierIds().forEach(modId -> {
						// 1. Correct the nesting: items -> modifier_groups -> options
						BigDecimal adjustment = menuDetails.stream()
								.flatMap(m -> m.getModifierGroupsList().stream())
								.flatMap(group -> group.getOptionsList().stream())
								.filter(option -> option.getOptionId().equals(modId.toString()))
								.findFirst()
								.map(option -> NumberUtil.stringToBigDecimal(option.getExtraPrice())) // String to BigDecimal
								.orElse(BigDecimal.ZERO);

						// 2. Add to your gRPC builder
						itemEntry.addModifiers(ModifierRequest.newBuilder()
								.setModifierId(modId.toString())
								.setPriceAdjustment(adjustment.toString())
								.build());
					});
				}

				orderRequestBuilder.addItems(itemEntry.build());
			});

			// Final gRPC Call
			orderGrpcClient.createOrder(orderRequestBuilder.build());

			return bookingMapper.toResponse(savedBooking);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException("System interrupted.");
		} finally {
			if (lock.isHeldByCurrentThread()) lock.unlock();
		}
	}

	@Override
	public BookingSetupDto.BookingSetupResponse getAvailableSlots(UUID branchId, OffsetDateTime date, int partySize, UUID currentUserId) {
		// 1. Setup Time Range for the Target Day
		OffsetDateTime startOfDay = date.truncatedTo(ChronoUnit.DAYS);
		OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

		// 2. Parallel Data Fetching
		var tablesFuture = CompletableFuture.supplyAsync(() -> restaurantGrpcClient.getBranchSlots(branchId, partySize));
		var scheduleFuture = CompletableFuture.supplyAsync(() -> restaurantGrpcClient.getBranchSchedule(branchId, date.getDayOfWeek().getValue()));
		var menuFuture = CompletableFuture.supplyAsync(() -> menuGrpcClient.getPreOrderMenu(branchId));

		// 3. Fetch Local Occupied Data
		List<Booking> occupied = bookingRepository.findAllByBranchIdAndDayRange(branchId, startOfDay, endOfDay);

		// 4. Combine Results
		List<TableSlotResponse> physicalTables = tablesFuture.join();
		ScheduleResponse schedule = scheduleFuture.join();
		List<MenuItemDetailResponse> menuItems = menuFuture.join();

		// 5. Generate and Filter Slots
		List<BookingSetupDto.BookingSetupResponse.AvailableSlot> slots = calculateSlots(date, physicalTables, occupied, schedule);

		return new BookingSetupDto.BookingSetupResponse(branchId, slots, mapMenu(menuItems));
	}

	@Override
	@Transactional
	public BookingDto.BookingResponse updateStatus(UUID bookingId, BookingStatus newStatus, String reason, UUID currentUserId, String userRoles) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new InvalidRequestException("Booking not found: " + bookingId));

		restaurantGrpcClient.validateBranchPermission(booking.getBranchId(), currentUserId, userRoles);

		BookingStatus oldStatus = booking.getStatus();

		// 1. Validation Logic
		if (oldStatus == BookingStatus.CANCELED || oldStatus == BookingStatus.COMPLETED) {
			throw new InvalidRequestException("Cannot change status of a finished or canceled booking.");
		}

		// 2. Update Entity
		booking.setStatus(newStatus);
		if (newStatus == BookingStatus.CANCELED) {
			booking.setCancellationReason(reason);
		}
		bookingRepository.save(booking);

		// 3. Save Audit History (Required for your booking_status_histories table)
		historyRepository.save(BookingStatusHistory.of(booking, oldStatus, currentUserId, reason));

		// 4. Publish Event for Microservices (Kafka)
		// This notifies the Restaurant Service to mark the table as OCCUPIED or ACTIVE
		eventProducer.sendBookingStatusUpdatedEvent(new BookingEvent.BookingStatusUpdatedEvent(
				booking.getId(),
				booking.getTableId(),
				booking.getBranchId(),
				booking.getUserId(),
				oldStatus,
				newStatus,
				booking.getBookingDatetime(),
				currentUserId,
				reason,
				OffsetDateTime.now()));

		return bookingMapper.toResponse(booking);
	}

	@Override
	@Transactional(readOnly = true)
	public BookingDto.BookingDetailResponse getBooking(UUID bookingId, UUID currentUserId, String userRoles) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new InvalidRequestException("Booking not found"));

		// Security check remains sequential as it's a prerequisite
		restaurantGrpcClient.validateBranchPermission(booking.getBranchId(), currentUserId, userRoles);

		return getEnrichedBookings(List.of(booking)).get(0);
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

		// 1. Security Check: Ensure the user owns the booking
		if (!booking.getUserId().equals(userId)) {
			throw new ForbiddenException("You are not authorized to cancel this booking");
		}

		// 2. Business Rule: Only allow cancellation if not already COMPLETED or CANCELED
		if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELED) {
			throw new InvalidRequestException("Booking cannot be canceled in its current state");
		}

		BookingStatus oldStatus = booking.getStatus();
		booking.setStatus(BookingStatus.CANCELED);
		booking.setCancellationReason(reason != null ? reason : "Canceled by user");

		bookingRepository.save(booking);

		// 3. Save Audit History (Required for your booking_status_histories table)
		historyRepository.save(BookingStatusHistory.of(booking, oldStatus, currentUserId, reason));

		// 3. Trigger the Audit and Waitlist Notification logic automatically
		eventProducer.sendBookingStatusUpdatedEvent(new BookingEvent.BookingStatusUpdatedEvent(
				bookingId,
				booking.getTableId(),
				booking.getBranchId(),
				booking.getUserId(),
				oldStatus,
				BookingStatus.CANCELED,
				booking.getBookingDatetime(),
				currentUserId,
				reason,
				OffsetDateTime.now()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingHistoryDto.BookingHistoryResponse> getStatusHistory(UUID bookingId) {
		// 1. Verify booking exists
		if (!bookingRepository.existsById(bookingId)) {
			throw new InvalidRequestException("Booking not found");
		}

		List<BookingStatusHistory> histories = historyRepository.findAllByBookingIdOrderByChangedAtDesc(bookingId);

		// 3. Map to DTOs
		return histories.stream()
				.map(bookingHistoryMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<BookingDto.BookingDetailResponse> getBookingsByBranch(UUID branchId, OffsetDateTime date, UUID currentUserId, String userRoles) {
		// 1. Define the start and end of the day for the filter
		OffsetDateTime startOfDay = date.truncatedTo(ChronoUnit.DAYS);
		OffsetDateTime endOfDay = startOfDay.plusDays(1);

		// 2. Fetch from repository using your SQL index
		List<Booking> bookings = bookingRepository.findAllByBranchIdAndBookingDatetimeBetween(
				branchId,
				startOfDay,
				endOfDay
		);

		if (bookings.isEmpty()) return List.of();

		return getEnrichedBookings(bookings);
	}

	@Transactional
	public BookingDto.BookingResponse markAsPaid(UUID bookingId, UUID staffOrSystemId, String paymentRef) {
		// 1. Fetch the booking
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new InvalidRequestException("Booking not found"));

		BookingStatus oldStatus = booking.getStatus();

		// 2. Validate current state (e.g., only CREATED bookings can be PAID)
		if (oldStatus != BookingStatus.CREATED) {
			log.warn("Booking {} already processed. Current status: {}", bookingId, oldStatus);
			return bookingMapper.toResponse(booking); // Return current state for idempotency
		}

		// 3. Update the state
		booking.setStatus(BookingStatus.PAID);
		bookingRepository.save(booking);

		// 4. Save Audit History (booking_status_histories table)
		historyRepository.save(BookingStatusHistory.of(
				booking,
				oldStatus,
				staffOrSystemId,
				"Payment confirmed. Ref: " + paymentRef
		));

		// 5. REGISTER ACTIONS AFTER COMMIT
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				// Send Kafka message ONLY after DB is updated
				eventProducer.sendBookingStatusUpdatedEvent(new BookingEvent.BookingStatusUpdatedEvent(
						booking.getId(),
						booking.getTableId(),
						booking.getBranchId(),
						booking.getUserId(),
						oldStatus,
						BookingStatus.PAID,
						booking.getBookingDatetime(),
						staffOrSystemId,
						"Payment confirmed. Ref: " + paymentRef,
						OffsetDateTime.now()));

				// Release lock ONLY after data is visible to other threads/services
				String lockKey = "lock:table:" + booking.getTableId();
				try {
					redissonClient.getLock(lockKey).unlock();
				} catch (IllegalMonitorStateException e) {
					log.info("Lock for table {} already released.", booking.getTableId());
				}
			}
		});

		return bookingMapper.toResponse(booking);
	}

//	@Override
//	@Transactional
//	public void confirmPayment(UUID bookingId) {
//		updateStatus(bookingId, BookingStatus.PAID, null, "Payment successfully processed");
//	}
//
//	@Override
//	@Transactional
//	public void cancelUnpaidBooking(UUID bookingId) {
//		updateStatus(bookingId, BookingStatus.CANCELED, null, "Canceled due to payment timeout");
//	}


	private List<BookingSetupDto.BookingSetupResponse.AvailableSlot> calculateSlots(
			OffsetDateTime date, List<TableSlotResponse> physical, List<Booking> occupied, ScheduleResponse schedule) {

		if (schedule.getClosed()) return Collections.emptyList();

		List<BookingSetupDto.BookingSetupResponse.AvailableSlot> result = new ArrayList<>();
		OffsetDateTime now = OffsetDateTime.now();

		// 1. Extract context from the OffsetDateTime 'date' parameter
		LocalDate requestedDate = date.toLocalDate();
		ZoneOffset offset = date.getOffset();

		// 2. Parse the gRPC Strings into LocalTime objects
		// Assumes format "HH:mm" (e.g., "09:00"). If it includes seconds, use LocalTime.parse(str)
		LocalTime openLocalTime = LocalTime.parse(schedule.getOpenTime());
		LocalTime closeLocalTime = LocalTime.parse(schedule.getCloseTime());

		// 3. Combine them to create valid OffsetDateTime boundaries
		OffsetDateTime openTime = requestedDate.atTime(openLocalTime).atOffset(offset);
		OffsetDateTime closeTime = requestedDate.atTime(closeLocalTime).atOffset(offset);

		// 4. Handle "Overnight" logic (if close is 02:00 AM, it's actually the next day)
		if (closeTime.isBefore(openTime)) {
			closeTime = closeTime.plusDays(1);
		}

		OffsetDateTime currentSlot = openTime;
		while (currentSlot.isBefore(closeTime)) {
			// Skip slots that are in the past
			if (currentSlot.isAfter(now)) {
				// Assume a standard 90-minute dining block for the NEW request
				OffsetDateTime slotEnd = currentSlot.plusMinutes(90);

				// Identify tables busy during THIS specific window
				final OffsetDateTime finalCurrentSlot = currentSlot;
				Set<UUID> busyIds = occupied.stream()
						.filter(b -> {
							OffsetDateTime bStart = b.getBookingDatetime();
							OffsetDateTime bEnd = bStart.plusMinutes(b.getDuration());
							// Overlap Algorithm: (StartA < EndB) AND (EndA > StartB)
							return finalCurrentSlot.isBefore(bEnd) && slotEnd.isAfter(bStart);
						})
						.map(Booking::getTableId)
						.collect(Collectors.toSet());

				List<BookingSetupDto.BookingSetupResponse.TableInfoDto> freeTables = physical.stream()
						.filter(pt -> !busyIds.contains(UUID.fromString(pt.getTableId())))
						.map(this::mapTableToDto)
						.toList();

				if (!freeTables.isEmpty()) {
					result.add(new BookingSetupDto.BookingSetupResponse.AvailableSlot(currentSlot, freeTables));
				}
			}
			currentSlot = currentSlot.plusMinutes(30); // 30-minute interval
		}
		return result;
	}

	private List<BookingSetupDto.BookingSetupResponse.MenuItemDto> mapMenu(List<MenuItemDetailResponse> menuItems) {
		if (menuItems == null) return Collections.emptyList();

		return menuItems.stream()
				.filter(MenuItemDetailResponse::getAvailable) // Only show items that are currently in stock
				.map(this::mapToDto)
				.toList();
	}

	private BookingSetupDto.BookingSetupResponse.MenuItemDto mapToDto(MenuItemDetailResponse item) {
		return BookingSetupDto.BookingSetupResponse.MenuItemDto.builder()
				.menuItemId(UUID.fromString(item.getMenuItemId()))
				.name(item.getName())
				.description(item.getDescription())
				.price(NumberUtil.stringToBigDecimal(item.getBasePrice()))
				.category(item.getCategory())
				// Map the repeated modifier groups
				.modifierGroups(item.getModifierGroupsList().stream()
						.map(group -> new BookingSetupDto.BookingSetupResponse.ModifierGroupDto(
								group.getGroupName(),
								group.getMinSelect(),
								group.getMaxSelect(),
								group.getOptionsList().stream() // Map nested options
										.map(opt -> new BookingSetupDto.BookingSetupResponse.ModifierOptionDto(
												opt.getName(),
												NumberUtil.stringToBigDecimal(opt.getExtraPrice())
										)).toList()
						)).toList())
				.build();
	}

	public void validateModifierSelections(List<BookingDto.BookingRequest.PreOrderItemRequest> selections, List<MenuItemDetailResponse> menuDetails) {
		for (var selection : selections) {
			// 1. Find source-of-truth menu detail
			MenuItemDetailResponse menu = menuDetails.stream()
					.filter(m -> m.getMenuItemId().equals(selection.menuItemId().toString()))
					.findFirst()
					.orElseThrow(() -> new BusinessException("Menu item not found: " + selection.menuItemId()));

			// 2. Validate each group (e.g., "Sides", "Toppings")
			for (ModifierGroupResponse group : menu.getModifierGroupsList()) {
				// Count how many of the user's modifierIds belong to this specific group
				long selectedInGroupCount = selection.modifierIds() == null ? 0 :
						selection.modifierIds().stream()
								.filter(id -> group.getOptionsList().stream()
										.anyMatch(opt -> opt.getOptionId().equals(id.toString())))
								.count();

				// Check Min rule (Mandatory)
				if (selectedInGroupCount < group.getMinSelect()) {
					throw new InvalidRequestException(String.format("Selection required for '%s' in '%s'", group.getGroupName(), menu.getName()));
				}

				// Check Max rule (Limit)
				if (group.getMaxSelect() > 0 && selectedInGroupCount > group.getMaxSelect()) {
					throw new InvalidRequestException(String.format("Too many selections for '%s' in '%s'", group.getGroupName(), menu.getName()));
				}
			}
		}
	}

	private BigDecimal calculateFinalPrice(List<BookingDto.BookingRequest.PreOrderItemRequest> selections, List<MenuItemDetailResponse> menuDetails) {
		BigDecimal grandTotal = BigDecimal.ZERO;

		for (var selection : selections) {
			MenuItemDetailResponse menu = menuDetails.stream()
					.filter(m -> m.getMenuItemId().equals(selection.menuItemId().toString()))
					.findFirst()
					.orElseThrow(() -> new BusinessException("Menu item mismatch"));

			// Base Price from gRPC
			BigDecimal itemPrice = NumberUtil.stringToBigDecimal(menu.getBasePrice());

			// Add Extra Prices for chosen modifiers
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

			// Multiply by quantity and add to grand total
			grandTotal = grandTotal.add(itemPrice.multiply(BigDecimal.valueOf(selection.quantity())));
		}
		return grandTotal;
	}

	private void saveBookingItems(Booking savedBooking,
	                              List<BookingDto.BookingRequest.PreOrderItemRequest> preOrderItems,
	                              List<MenuItemDetailResponse> menuDetails) {
		if (preOrderItems == null || preOrderItems.isEmpty()) return;

		List<BookingItem> entities = preOrderItems.stream().map(itemDto -> {
			// 1. Find the specific menu item from gRPC data to get real prices
			MenuItemDetailResponse menu = menuDetails.stream()
					.filter(m -> m.getMenuItemId().equals(itemDto.menuItemId().toString()))
					.findFirst()
					.orElseThrow(() -> new BusinessException("Item mismatch"));

			// 2. Calculate the Unit Price (Base Price + all selected Modifier Prices)
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

			// 3. Build the entity matching your SQL columns
			return BookingItem.builder()
					.booking(savedBooking)
					.menuItemId(itemDto.menuItemId())
					.quantity(itemDto.quantity())
					.unitPrice(unitPrice) // Calculated server-side for security
					.totalPrice(unitPrice.multiply(BigDecimal.valueOf(itemDto.quantity())))
					.notes(itemDto.notes())
					// .modifierIds(itemDto.modifierIds()) // Requires the ALTER TABLE step above
					.build();
		}).toList();

		bookingItemRepository.saveAll(entities);
	}

	private List<BookingDto.BookingDetailResponse> getEnrichedBookings(List<Booking> bookings) {
		// 1. Collect all unique IDs to avoid redundant gRPC calls
		Set<String> userIds = bookings.stream().map(b -> b.getUserId().toString()).collect(Collectors.toSet());
		Set<String> tableIds = bookings.stream().map(b -> b.getTableId().toString()).collect(Collectors.toSet());
		List<String> menuItemIds = bookings.stream()
				.flatMap(b -> b.getItems().stream().map(i -> i.getMenuItemId().toString()))
				.distinct().toList();

		// 2. Fetch all external data in parallel
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

		CompletableFuture.allOf(usersFuture, tablesFuture, menusFuture).join();

		var userMap = usersFuture.join();
		var tableMap = tablesFuture.join();
		var menuMap = menusFuture.join();

		// 3. Map and return
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
								.setTableId(NIL_UUID)      // Result: "00000000..."
								.setZoneId(NIL_UUID)       // Result: "00000000..."
								.setBranchId(NIL_UUID)     // Result: "00000000..."
								.setRestaurantId(NIL_UUID) // Result: "00000000..."
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

	private BookingSetupDto.BookingSetupResponse.TableInfoDto mapTableToDto(TableSlotResponse grpcTable) {
		return new BookingSetupDto.BookingSetupResponse.TableInfoDto(
				UUID.fromString(grpcTable.getTableId()),
				grpcTable.getTableLabel(),
				grpcTable.getZoneName(),
				grpcTable.getMaxCapacity()
		);
	}

}
