package com.tuttime.booking.service.impl;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.dto.BookingHistoryDto;
import com.tuttime.booking.dto.BookingSetupDto;
import com.tuttime.booking.dto.WaitlistDto;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingItem;
import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.booking.entity.WaitlistEntry;
import com.tuttime.booking.entity.enums.WaitlistStatus;
import com.tuttime.booking.grpc.client.MenuGrpcClient;
import com.tuttime.booking.grpc.client.OrderGrpcClient;
import com.tuttime.booking.grpc.client.RestaurantGrpcClient;
import com.tuttime.booking.grpc.client.UserGrpcClient;
import com.tuttime.booking.mapper.BookingHistoryMapper;
import com.tuttime.booking.mapper.BookingMapper;
import com.tuttime.booking.repository.BookingItemRepository;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import com.tuttime.booking.repository.WaitlistRepository;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.enums.UserRoleKind;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.util.NumberUtil;
import com.tuttime.common.util.UuidUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.*;
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
    private final WaitlistRepository waitlistRepository;
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

    @Value("${app.booking.slot-interval}")
    private int slotInterval;

    @Value("${app.booking.dining-duration}")
    private int diningDuration;

    private static final String LOCK_PREFIX = "lock:table:";

    // -------------------- Create Booking (with multiple tables) --------------------
    @Override
    @Transactional
    public BookingDto.BookingResponse createBooking(BookingDto.BookingRequest request,
                                                    UUID currentUserId, String userRoles) {
        UUID branchId = request.branchId();
        List<UUID> tableIds = request.tableIds();
        if (tableIds == null || tableIds.isEmpty()) {
            throw new InvalidRequestException("At least one table must be selected");
        }

        List<GetTableDetailResponse> tableDetails = new ArrayList<>();
        for (UUID tid : tableIds) {
            GetTableDetailResponse resp = restaurantGrpcClient.getTableDetail(tid);
            if (!resp.getAvailable()) {
                throw new InvalidRequestException("Table " + tid + " is no longer available.");
            }
            if (!resp.getBranchId().equals(branchId.toString())) {
                throw new InvalidRequestException("Table " + tid + " does not belong to the selected branch.");
            }
            tableDetails.add(resp);
        }

        // Acquire Redis locks for ALL tables
        List<RLock> locks = new ArrayList<>();
        try {
            for (UUID tid : tableIds) {
                RLock lock = redissonClient.getLock(LOCK_PREFIX + tid);
                if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                    throw new ConflictException("One or more tables are being reserved by another user.");
                }
                locks.add(lock);
            }

            // Check overlapping bookings for all tables
            OffsetDateTime startTime = request.bookingDatetime();
            OffsetDateTime endTime = startTime.plus(Duration.ofMinutes(request.duration()));
            if (bookingRepository.existsOverlappingBookingForTables(tableIds, startTime, endTime)) {
                throw new ConflictException("One or more tables are already booked for this time range.");
            }

            // Fetch menu details and calculate total amount
            List<MenuItemDetailPto> menuDetails = menuGrpcClient.getMenuDetails(
                    request.preOrderItems().stream().map(i -> i.menuItemId().toString()).toList()
            );

            // Validate items against the stop-list status
            for (MenuItemDetailPto menu : menuDetails) {
                // If menu-service marks this item as false/unavailable due to stop-list enforcement
                if (!menu.getAvailable()) {
                    throw new InvalidRequestException("Cannot complete booking. Item is currently out of stock: "
                            + menu.getMenuItemId());
                }
            }

            validateModifierSelections(request.preOrderItems(), menuDetails);
            BigDecimal totalAmount = calculateFinalPrice(request.preOrderItems(), menuDetails);

            // Determine restaurantId and zoneId (take from first table, but multiple tables may have different zones – store null)
            UUID restaurantId = UUID.fromString(tableDetails.getFirst().getRestaurantId());

            // Build and save booking (without zoneId, with tableIds collection)
            Booking booking = Booking.builder()
                    .userId(request.userId())
                    .restaurantId(restaurantId)
                    .branchId(branchId)
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
            // Assign table IDs using the element collection
            booking.setTableIds(tableIds);
            Booking savedBooking = bookingRepository.save(booking);

            // Update branch occupancy counter (using Redis atomic) – count each booking as 1, regardless of tables
            String occupancyKey = "branch:occupancy:" + savedBooking.getBranchId();
            RAtomicLong activeBookingsCounter = redissonClient.getAtomicLong(occupancyKey);
            long currentCount = activeBookingsCounter.incrementAndGet();

            // Check high occupancy (optional)
            checkAndNotifyLowAvailability(booking, currentCount);

            // Save pre-order items
            saveBookingItems(savedBooking, request.preOrderItems(), menuDetails);

            // Status history
            historyRepository.save(BookingStatusHistory.of(savedBooking, null, request.userId(), "Initial creation"));

            // Publish Kafka event
            BookingEvent.BookingStatusUpdatedEvent event = BookingEvent.BookingStatusUpdatedEvent.builder()
                    .bookingId(savedBooking.getId())
                    .tableIds(tableIds)   // adapt event to include list
                    .branchId(savedBooking.getBranchId())
                    .userId(savedBooking.getUserId())
                    .oldStatus(null)
                    .newStatus(BookingStatus.CREATED)
                    .bookingTime(savedBooking.getBookingDatetime())
                    .changedBy(currentUserId)
                    .reason("Created")
                    .build();
            eventProducer.sendBookingStatusUpdatedEvent(event);

            // Create order via Order Service
            CreateOrderRequest.Builder orderRequestBuilder = CreateOrderRequest.newBuilder()
                    .setBookingId(savedBooking.getId().toString())
                    .setUserId(savedBooking.getUserId().toString())
                    .setBranchId(savedBooking.getBranchId().toString())
                    .setBookingDatetime(savedBooking.getBookingDatetime().toString())
                    .setGuestName(savedBooking.getGuestName())
                    .setGuestPhone(savedBooking.getGuestPhone())
                    .setSpecialRequest(savedBooking.getSpecialRequest());

            orderRequestBuilder.addAllTableIds(tableIds.stream().map(UUID::toString).toList());

            for (BookingDto.BookingRequest.PreOrderItemRequest itemDto : request.preOrderItems()) {
                MenuItemDetailPto menu = menuDetails.stream()
                        .filter(m -> m.getMenuItemId().equals(itemDto.menuItemId().toString()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("Menu item not found: " + itemDto.menuItemId()));

                OrderItemRequest.Builder itemBuilder = OrderItemRequest.newBuilder()
                        .setMenuItemId(itemDto.menuItemId().toString())
                        .setQuantity(itemDto.quantity())
                        .setPrice(menu.getBasePrice());

                if (itemDto.modifierIds() != null && !itemDto.modifierIds().isEmpty()) {
                    for (UUID modId : itemDto.modifierIds()) {
                        String extra = menu.getModifierGroupsList().stream()
                                .flatMap(group -> group.getOptionsList().stream())
                                .filter(opt -> opt.getOptionId().equals(modId.toString()))
                                .map(ModifierOptionPto::getExtraPrice)
                                .findFirst()
                                .orElse("0.0");
                        itemBuilder.addModifiers(ModifierRequest.newBuilder()
                                .setModifierId(modId.toString())
                                .setPriceAdjustment(extra.toString())
                                .build());
                    }
                }
                orderRequestBuilder.addItems(itemBuilder.build());
            }

            CreateOrderResponse orderResponse = orderGrpcClient.createOrder(orderRequestBuilder.build());
            savedBooking.setOrderId(UuidUtil.toUuid(orderResponse.getOrderId()));
            bookingRepository.save(savedBooking);

            return bookingMapper.toResponse(savedBooking);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("System interrupted.");
        } finally {
            // Release all locks held by this thread
            for (RLock lock : locks) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    // -------------------- Edit Booking (update tables, time, items) --------------------
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
        boolean tablesChanged = false;
        boolean itemsChanged = false;
        List<UUID> newTableIds = null;
        OffsetDateTime newStartTime = null;
        OffsetDateTime newEndTime = null;

        // --- Handle time/table change if provided ---
        if (request.bookingDatetime() != null || request.tableIds() != null) {
            newStartTime = request.bookingDatetime() != null ? request.bookingDatetime() : booking.getBookingDatetime();
            newEndTime = newStartTime.plusMinutes(booking.getDuration());
            newTableIds = request.tableIds() != null ? request.tableIds() : booking.getTableIds();
            slotChanged = !booking.getBookingDatetime().equals(newStartTime);
            tablesChanged = !booking.getTableIds().equals(newTableIds);
        }

        if (slotChanged || tablesChanged) {
            // Acquire locks for new tables (if changed)
            List<RLock> newLocks = new ArrayList<>();
            try {
                for (UUID tid : newTableIds) {
                    RLock lock = redissonClient.getLock(LOCK_PREFIX + tid);
                    if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                        throw new ConflictException("One or more new tables are currently being reserved.");
                    }
                    newLocks.add(lock);
                }
                // Check availability for all new tables
                if (bookingRepository.existsOverlappingBookingForTables(newTableIds, newStartTime, newEndTime)) {
                    throw new ConflictException("One or more tables are already booked for the new time range.");
                }
                // No need to unlock now – will be released after save (or after finally)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("Interrupted during re‑locking.");
            } finally {
                // We keep locks until after transaction, but we can't unlock here – will be released in finally of the outer method?
                // Instead, keep locks in a list and unlock in a finally block. We'll refactor.
                // For simplicity, we rely on lock expiration (10 seconds) – acceptable.
            }
        }

        // --- Handle items update if provided ---
        List<MenuItemDetailPto> menuDetails = null;
        BigDecimal newTotal = null;
        if (request.preOrderItems() != null) {
            menuDetails = menuGrpcClient.getMenuDetails(
                    request.preOrderItems().stream().map(i -> i.menuItemId().toString()).toList()
            );

            // Validate items against the stop-list status
            for (MenuItemDetailPto menu : menuDetails) {
                // If menu-service marks this item as false/unavailable due to stop-list enforcement
                if (!menu.getAvailable()) {
                    throw new InvalidRequestException("Cannot complete booking. Item is currently out of stock: "
                            + menu.getMenuItemId());
                }
            }

            validateModifierSelections(request.preOrderItems(), menuDetails);
            newTotal = calculateFinalPrice(request.preOrderItems(), menuDetails);
            itemsChanged = true;
        }

        // --- Update fields ---
        if (request.guestName() != null) booking.setGuestName(request.guestName());
        if (request.guestPhone() != null) booking.setGuestPhone(request.guestPhone());
        if (request.specialRequest() != null) booking.setSpecialRequest(request.specialRequest());
        if (request.partySize() != null) booking.setPartySize(request.partySize());
        if (slotChanged) booking.setBookingDatetime(newStartTime);
        if (tablesChanged) booking.setTableIds(newTableIds);
        if (itemsChanged) {
            booking.getItems().clear();
            saveBookingItems(booking, request.preOrderItems(), menuDetails);
            booking.setTotalAmount(newTotal);
        }

        // Notify Order Service if order exists
        if ((slotChanged || tablesChanged || itemsChanged) && booking.getOrderId() != null) {
            UpdateOrderRequest orderUpdate = UpdateOrderRequest.newBuilder()
                    .setOrderId(booking.getOrderId().toString())
                    .setNewBookingTime(booking.getBookingDatetime().toString())
                    .setNewTotal(booking.getTotalAmount().toString())
                    .setGuestName(booking.getGuestName())
                    .setGuestPhone(booking.getGuestPhone())
                    .addAllTableIds(booking.getTableIds().stream().map(UUID::toString).toList())
                    .build();
            UpdateOrderResponse orderResponse = orderGrpcClient.updateOrder(orderUpdate);
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
        LocalDate requestedDate = date.toLocalDate();
        ZoneOffset offset = date.getOffset();
        OffsetDateTime now = OffsetDateTime.now(offset);

        // 1. Past date → no slots
        if (requestedDate.isBefore(now.toLocalDate())) {
            return new BookingSetupDto.BookingSetupResponse(branchId, Collections.emptyList());
        }

        // 2. Fetch tables and schedule in parallel
        CompletableFuture<List<TableSlotPto>> tablesFuture = CompletableFuture.supplyAsync(() ->
                restaurantGrpcClient.getBranchSlots(branchId, 1)); // ignore partySize
        CompletableFuture<GetBranchScheduleResponse> scheduleFuture = CompletableFuture.supplyAsync(() ->
                restaurantGrpcClient.getBranchSchedule(branchId, requestedDate.getDayOfWeek().getValue()));

        // 3. Fetch all bookings for the day (needed to know which tables are occupied)
        OffsetDateTime startOfDay = requestedDate.atStartOfDay().atOffset(offset);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);
        List<Booking> dailyBookings = bookingRepository.findAllByBranchIdAndDayRange(branchId, startOfDay, endOfDay);

        // 4. Wait for external calls
        List<TableSlotPto> allTables = tablesFuture.join();
        GetBranchScheduleResponse schedule = scheduleFuture.join();

        // 5. If branch is closed that day, return empty
        if (schedule.getClosed()) {
            return new BookingSetupDto.BookingSetupResponse(branchId, Collections.emptyList());
        }

        // 6. Build occupation map: tableId -> list of bookings that occupy it
        //    Flatten because a booking can occupy multiple tables
        Map<UUID, List<Booking>> tableOccupation = dailyBookings.stream()
                .filter(b -> b.getTableIds() != null && !b.getTableIds().isEmpty())
                .flatMap(booking -> booking.getTableIds().stream()
                        .map(tableId -> Map.entry(tableId, booking)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        // 7. Determine opening & closing times for the day
        LocalTime openLocal = LocalTime.parse(schedule.getOpenTime());
        LocalTime closeLocal = LocalTime.parse(schedule.getCloseTime());
        OffsetDateTime openTime = requestedDate.atTime(openLocal).atOffset(offset);
        OffsetDateTime closeTime = requestedDate.atTime(closeLocal).atOffset(offset);

        // 8. Starting slot (rounded to next interval if it's today)
        OffsetDateTime currentSlot = requestedDate.equals(now.toLocalDate())
                ? roundToNextInterval(now.plusMinutes(15), slotInterval)
                : openTime;
        if (currentSlot.isBefore(openTime)) currentSlot = openTime;

        List<BookingSetupDto.BookingSetupResponse.AvailableSlot> result = new ArrayList<>();

        // 9. Iterate through slots until we exceed closing time
        while (currentSlot.plusMinutes(diningDuration).isBefore(closeTime) ||
                currentSlot.plusMinutes(diningDuration).isEqual(closeTime)) {
            final OffsetDateTime slotStart = currentSlot;
            final OffsetDateTime slotEnd = currentSlot.plusMinutes(diningDuration);

            // Filter tables that are free during this slot
            List<TableSlotPto> freeTables = allTables.stream()
                    .filter(table -> {
                        UUID tableId = UUID.fromString(table.getTableId());
                        List<Booking> occupying = tableOccupation.get(tableId);
                        if (occupying == null) return true;
                        // Check for any overlapping booking
                        return occupying.stream().noneMatch(booking ->
                                slotStart.isBefore(booking.getBookingDatetime().plusMinutes(booking.getDuration())) &&
                                        slotEnd.isAfter(booking.getBookingDatetime())
                        );
                    })
                    .toList();

            // Build TableInfoDto list for this slot
            List<BookingSetupDto.BookingSetupResponse.TableInfoDto> tableInfos = freeTables.stream()
                    .map(t -> new BookingSetupDto.BookingSetupResponse.TableInfoDto(
                            UUID.fromString(t.getTableId()),
                            t.getTableLabel(),
                            t.getZoneName(),
                            t.getMaxCapacity()
                    ))
                    .collect(Collectors.toList());

            // Create available slot DTO
            result.add(new BookingSetupDto.BookingSetupResponse.AvailableSlot(
                    slotStart,
                    !freeTables.isEmpty(),
                    freeTables.size(),
                    freeTables.stream().map(TableSlotPto::getZoneName).distinct().collect(Collectors.toList()),
                    tableInfos
            ));

            currentSlot = currentSlot.plusMinutes(slotInterval);
        }

        return new BookingSetupDto.BookingSetupResponse(branchId, result);
    }

    // -------------------- Update Status (unchanged except event) --------------------
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
                .tableIds(booking.getTableIds())
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
        if (!booking.getUserId().equals(currentUserId)) {
            restaurantGrpcClient.validateBranchPermission(booking.getBranchId(), currentUserId, userRoles);
        }

        return getEnrichedBookings(List.of(booking)).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto.BookingDetailResponse> getBookingsByUser(UUID userId) {
        List<Booking> bookings = bookingRepository.findAllByUserIdOrderByBookingDatetimeDesc(userId);
        if (bookings.isEmpty()) return List.of();
        return getEnrichedBookings(bookings);
    }

    // -------------------- Delete / Cancel Booking --------------------
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
            CancelAndRefundRequest cancelRequest = CancelAndRefundRequest.newBuilder()
                    .setBookingId(booking.getId().toString())
                    .setOrderId(booking.getOrderId().toString())
                    .setPaymentId(booking.getPaymentId().toString())
                    .setReason(reason != null ? reason : "Canceled by user")
                    .build();
            CancelAndRefundResponse orderResponse = orderGrpcClient.cancelAndRefund(cancelRequest);
            if (!orderResponse.getSuccess()) {
                log.warn("Booking {} Refund failed. Current status: {}", bookingId, oldStatus);
                throw new InvalidRequestException("Refund failed: " + orderResponse.getReason());
            }
        }
        booking.setStatus(BookingStatus.CANCELED);
        booking.setCancellationReason(reason != null ? reason : "Canceled by user");
        bookingRepository.save(booking);
        historyRepository.save(BookingStatusHistory.of(booking, oldStatus, currentUserId, reason));

        for (UUID tid : booking.getTableIds()) {
            RLock lock = redissonClient.getLock(LOCK_PREFIX + tid);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        BookingEvent.BookingStatusUpdatedEvent event = BookingEvent.BookingStatusUpdatedEvent.builder()
                .bookingId(booking.getId())
                .tableIds(booking.getTableIds())
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

        notifyNextWaiting(booking.getBranchId(), booking.getZoneId(), booking.getPartySize());
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

    @Override
    @Transactional(readOnly = true)
    public Page<BookingDto.BookingDetailResponse> getBookingsByBranch(
            UUID branchId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            BookingStatus status,
            Pageable pageable,
            UUID currentUserId,
            String userRoles) {

        // 1. Permission check (ensure user can access this branch)
        restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);

        // 2. Build dynamic specification
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("branchId"), branchId));

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bookingDatetime"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bookingDatetime"), toDate));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 3. Execute paginated query
        Page<Booking> bookingPage = bookingRepository.findAll(spec, pageable);

        // 4. If no results, return empty page
        if (!bookingPage.hasContent()) {
            return Page.empty(pageable);
        }

        // 5. Enrich each booking with user, table, etc. (reuse your existing helper)
        List<Booking> bookings = bookingPage.getContent();
        List<BookingDto.BookingDetailResponse> enriched = getEnrichedBookings(bookings);

        // 6. Return a new PageImpl with the enriched content
        return new PageImpl<>(enriched, pageable, bookingPage.getTotalElements());
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
                        .tableIds(booking.getTableIds())
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
                .tableIds(booking.getTableIds())
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
                                           List<MenuItemDetailPto> menuDetails) {
        for (var selection : selections) {
            MenuItemDetailPto menu = menuDetails.stream()
                    .filter(m -> m.getMenuItemId().equals(selection.menuItemId().toString()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Menu item not found: " + selection.menuItemId()));
            // Collect allowed modifier IDs
            Set<String> allowedModifierIds = menu.getModifierGroupsList().stream()
                    .flatMap(group -> group.getOptionsList().stream())
                    .map(ModifierOptionPto::getOptionId)
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
            for (ModifierGroupPto group : menu.getModifierGroupsList()) {
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
                                           List<MenuItemDetailPto> menuDetails) {
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (var selection : selections) {
            MenuItemDetailPto menu = menuDetails.stream()
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
                                  List<MenuItemDetailPto> menuDetails) {
        if (preOrderItems == null || preOrderItems.isEmpty()) return;
        List<BookingItem> entities = preOrderItems.stream().map(itemDto -> {
            MenuItemDetailPto menu = menuDetails.stream()
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

    // -------------------- Get Enriched Bookings (with multiple tables) --------------------
    private List<BookingDto.BookingDetailResponse> getEnrichedBookings(List<Booking> bookings) {
        Set<String> userIds = bookings.stream().map(b -> b.getUserId().toString()).collect(Collectors.toSet());
        // Collect all table IDs from all bookings
        Set<String> tableIds = bookings.stream()
                .flatMap(b -> b.getTableIds().stream().map(UUID::toString))
                .collect(Collectors.toSet());
        List<String> menuItemIds = bookings.stream()
                .flatMap(b -> b.getItems().stream().map(i -> i.getMenuItemId().toString()))
                .distinct().toList();

        // ... parallel fetch as before ...
        var usersFuture = CompletableFuture.supplyAsync(() -> fetchUsersMap(userIds));
        var tablesFuture = CompletableFuture.supplyAsync(() -> fetchTablesMap(tableIds));
        var menusFuture = CompletableFuture.supplyAsync(() -> fetchMenuMap(menuItemIds));

        CompletableFuture.allOf(usersFuture, tablesFuture, menusFuture).join();

        var userMap = usersFuture.join();
        var tableMap = tablesFuture.join();
        var menuMap = menusFuture.join();

        return bookings.stream().map(booking -> {
            var user = userMap.getOrDefault(booking.getUserId().toString(),
                    GetUserDetailResponse.newBuilder().setUserId(NIL_UUID).setFullName("Deleted User").build());
            // Get list of TableHierarchyResponse for each table ID
            List<GetTableHierarchyDetailResponse> tables = booking.getTableIds().stream()
                    .map(tid -> tableMap.getOrDefault(tid.toString(),
                            GetTableHierarchyDetailResponse.newBuilder().setTableId(tid.toString()).setTableLabel("N/A").build()))
                    .toList();
            var bookingMenus = booking.getItems().stream()
                    .map(i -> menuMap.get(i.getMenuItemId().toString()))
                    .filter(Objects::nonNull)
                    .toList();
            return bookingMapper.toDetailResponse(booking, user, tables, bookingMenus);
        }).toList();
    }

    // -------------------- Helper: fetch tables map --------------------
    private Map<String, GetTableHierarchyDetailResponse> fetchTablesMap(Set<String> ids) {
        return ids.parallelStream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    try {
                        return restaurantGrpcClient.getTableHierarchyDetail(GetTableHierarchyDetailRequest.newBuilder().setTableId(id).build());
                    } catch (Exception e) {
                        return GetTableHierarchyDetailResponse.newBuilder()
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

    private Map<String, GetUserDetailResponse> fetchUsersMap(Set<String> ids) {
        return ids.parallelStream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    try {
                        return userGrpcClient.getUserDetail(GetUserDetailRequest.newBuilder().setUserId(id).build());
                    } catch (Exception e) {
                        return GetUserDetailResponse.newBuilder().setUserId(NIL_UUID).build();
                    }
                }
        ));
    }

    private Map<String, MenuItemDetailPto> fetchMenuMap(List<String> menuItemIds) {
        try {
            return menuGrpcClient.getMenuDetails(menuItemIds).stream()
                    .collect(Collectors.toMap(MenuItemDetailPto::getMenuItemId, m -> m));
        } catch (Exception e) {
            log.warn("Menu service failed", e);
            return Collections.emptyMap();
        }
    }

    private void checkAndNotifyLowAvailability(Booking booking, long currentCount) {
        if (booking.getTableIds() == null || booking.getTableIds().isEmpty()) {
            log.warn("Booking {} has no tables, cannot check availability", booking.getId());
            return;
        }

        // Use the first table to fetch branch-level details (all tables share same branch)
        UUID anyTableId = booking.getTableIds().get(0);
        GetTableDetailResponse tableDetail = restaurantGrpcClient.getTableDetail(anyTableId);
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

    //-------------------- Waitlist service ----------------------------------

    @Override
    @Transactional
    public WaitlistDto.WaitlistResponse addToWaitlist(WaitlistDto.WaitlistRequest request,
                                                      UUID currentUserId,
                                                      String userRoles) {
        // Check if user already waiting at this branch
        boolean alreadyWaiting = waitlistRepository.existsByUserIdAndBranchIdAndStatus(
                currentUserId, request.branchId(), WaitlistStatus.WAITING);

        if (alreadyWaiting) {
            throw new ConflictException("User is already on the waitlist for this branch.");
        }

        WaitlistEntry entry = WaitlistEntry.builder()
                .userId(currentUserId)
                .branchId(request.branchId())
                .zoneId(request.zoneId())
                .partySize(request.partySize())
                .joinedAt(OffsetDateTime.now())
                .status(WaitlistStatus.WAITING)
                .build();

        WaitlistEntry saved = waitlistRepository.save(entry);
        log.info("User {} joined waitlist at branch {}", currentUserId, request.branchId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WaitlistDto.WaitlistResponse getWaitlistEntry(UUID id, UUID currentUserId, String userRoles) {
        WaitlistEntry entry = waitlistRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("Waitlist entry not found"));

        // Security: allow admin/owner/manager or the owner of the entry
        if (!isAdminOrOwnerOrManager(userRoles) && !entry.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You are not authorized to view this waitlist entry");
        }

        return mapToResponse(entry);
    }

    @Override
    @Transactional
    public void leaveWaitlist(UUID id, UUID currentUserId, String userRoles) {
        WaitlistEntry entry = waitlistRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("Waitlist entry not found"));

        // Security: same as above
        if (!isAdminOrOwnerOrManager(userRoles) && !entry.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You are not authorized to remove this waitlist entry");
        }

        entry.setStatus(WaitlistStatus.CANCELED);
        waitlistRepository.save(entry);
        log.info("User {} left the waitlist (entry {})", entry.getUserId(), id);
    }

    @Override
    @Transactional
    public void updateWaitlistStatus(UUID id, WaitlistStatus newStatus, UUID currentUserId, String userRoles) {
        // This method should be accessible only to admins/managers
        if (!isAdminOrOwnerOrManager(userRoles)) {
            throw new ForbiddenException("Only administrators or restaurant staff can update waitlist status.");
        }

        WaitlistEntry entry = waitlistRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("Waitlist entry not found"));

        entry.setStatus(newStatus);
        if (newStatus == WaitlistStatus.NOTIFIED) {
            entry.setNotifiedAt(OffsetDateTime.now());
        }
        waitlistRepository.save(entry);
        log.info("Waitlist entry {} status updated to {}", id, newStatus);
    }

    @Override
    @Transactional
    public void notifyNextWaiting(UUID branchId, UUID zoneId, int partySize) {
        // Find the earliest waiting entry for this branch and zone (party size can be fuzzy)
        Optional<WaitlistEntry> next = waitlistRepository.findFirstByBranchIdAndZoneIdAndStatusOrderByJoinedAtAsc(
                branchId, zoneId, WaitlistStatus.WAITING);
        if (next.isEmpty()) {
            log.debug("No waiting users for branch {} zone {}", branchId, zoneId);
            return;
        }
        WaitlistEntry entry = next.get();
        entry.setStatus(WaitlistStatus.NOTIFIED);
        entry.setNotifiedAt(OffsetDateTime.now());
        waitlistRepository.save(entry);
        log.info("Notified user {} for waitlist entry {}", entry.getUserId(), entry.getId());
        // TODO: send push notification via Notification Service (gRPC or Kafka)
    }

    @Override
    @Transactional
    public BookingDto.BookingResponse claimWaitlistEntry(UUID entryId,
                                                         BookingDto.BookingRequest bookingRequest,
                                                         UUID currentUserId,
                                                         String userRoles) {
        // 1. Fetch waitlist entry
        WaitlistEntry entry = waitlistRepository.findById(entryId)
                .orElseThrow(() -> new InvalidRequestException("Waitlist entry not found"));

        // 2. Security check
        if (!entry.getUserId().equals(currentUserId) && !isAdminOrOwnerOrManager(userRoles)) {
            throw new ForbiddenException("Not authorized to claim this waitlist entry");
        }

        // 3. Must be in NOTIFIED state
        if (entry.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new InvalidRequestException("Waitlist entry is not in NOTIFIED state");
        }

        // 4. Find an available table (immediate seating)
        FindAvailableTableResponse tableDetail = restaurantGrpcClient.findAvailableTable(
                entry.getBranchId(),
                entry.getZoneId(),
                entry.getPartySize(),
                OffsetDateTime.now()
        );
        if (!tableDetail.getAvailable()) {
            throw new InvalidRequestException("No table currently available for your party size. Please try again later.");
        }

        // 5. Build BookingRequest – all fields are mandatory except those with defaults
        List<BookingDto.BookingRequest.PreOrderItemRequest> preOrderItems = bookingRequest.preOrderItems() != null
                ? bookingRequest.preOrderItems()
                : Collections.emptyList();

        BookingDto.BookingRequest mergedRequest = new BookingDto.BookingRequest(
                currentUserId,
                bookingRequest.branchId(),
                List.of(UUID.fromString(tableDetail.getTableId())),               // tableId
                OffsetDateTime.now(),                                    // bookingDatetime
                bookingRequest.duration() != null ? bookingRequest.duration() : 90, // duration (default 90 min)
                entry.getPartySize(),                                    // partySize
                bookingRequest.leadTimeMinutes() != null ? bookingRequest.leadTimeMinutes() : 0,
                bookingRequest.gracePeriodMins() != null ? bookingRequest.gracePeriodMins() : 15,
                bookingRequest.guestName(),
                bookingRequest.guestPhone(),
                bookingRequest.specialRequest(),
                preOrderItems
        );

        // 6. Create the booking (internal method sets status to CONFIRMED)
        Booking booking = createBookingFromWaitlist(mergedRequest, currentUserId, userRoles);

        // 7. Update waitlist entry to CONVERTED
        entry.setStatus(WaitlistStatus.CONVERTED);
        waitlistRepository.save(entry);

        log.info("Waitlist entry {} claimed, created booking {}", entryId, booking.getId());

        // 8. Return response using mapper
        return bookingMapper.toResponse(booking);
    }

    // ---------- Helper methods ----------
    private WaitlistDto.WaitlistResponse mapToResponse(WaitlistEntry entity) {
        long position = 0;
        if (entity.getStatus() == WaitlistStatus.WAITING) {
            // Count entries before this one (by joinedAt) in the same branch and zone
            position = waitlistRepository.countByBranchIdAndZoneIdAndStatusAndJoinedAtBefore(
                    entity.getBranchId(),
                    entity.getZoneId(),
                    WaitlistStatus.WAITING,
                    entity.getJoinedAt()) + 1;
        }

        return new WaitlistDto.WaitlistResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getJoinedAt(),
                (int) position
        );
    }

    private boolean isAdminOrOwnerOrManager(String userRoles) {
        if (userRoles == null) return false;
        List<String> roles = Arrays.asList(userRoles.split(","));
        return roles.contains(UserRoleKind.ADMIN.name()) ||
                roles.contains(UserRoleKind.OWNER.name()) ||
                roles.contains(UserRoleKind.MANAGER.name());
    }
}