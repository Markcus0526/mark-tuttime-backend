package com.tuttime.analytics.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.analytics.dto.AdminDto;
import com.tuttime.analytics.dto.RestaurantDto;
import com.tuttime.analytics.entity.BranchYieldManagement;
import com.tuttime.analytics.entity.FinancialLedger;
import com.tuttime.analytics.entity.SystemEvent;
import com.tuttime.analytics.entity.UserReliabilityIndex;
import com.tuttime.analytics.grpc.client.*;
import com.tuttime.analytics.mapper.AdminMapper;
import com.tuttime.analytics.mapper.RestaurantMapper;
import com.tuttime.analytics.repository.*;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.common.event.kafka.event.PaymentEvent;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tuttime.analytics.constants.AnalyticsConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SystemEventRepository systemEventsRepository;
    private final UserReliabilityRepository reliabilityRepository;
    private final BranchYieldManagementRepository yieldRepository;
    private final FinancialLedgerRepository financialLedgerRepository;
    private final BranchPerformanceStatRepository branchPerformanceStatRepository;
    private final AdminMapper adminMapper;
    private final RestaurantMapper restaurantMapper;
    private final RestaurantGrpcClient restaurantGrpcClient;
    private final OrderGrpcClient orderGrpcClient;
    private final MenuGrpcClient menuGrpcClient;
    private final BookingGrpcClient bookingGrpcClient;
    private final PosGrpcClient posGrpcClient;
    private final PaymentGrpcClient paymentGrpcClient;
    private final UserGrpcClient userGrpcClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    // ---------- Admin Queries ----------
    @Override
    @Transactional(readOnly = true)
    public AdminDto.GlobalMetricsResponse getPlatformSummary() {
        BigDecimal gmv = financialLedgerRepository.sumTotalGmv();
        BigDecimal commission = financialLedgerRepository.sumTotalCommission();
        Double yield = yieldRepository.calculateAveragePlatformOccupancy();

        return AdminDto.GlobalMetricsResponse.builder()
                .totalGmv(gmv != null ? gmv : BigDecimal.ZERO)
                .totalCommission(commission != null ? commission : BigDecimal.ZERO)
                .activeBranchesCount(financialLedgerRepository.countDistinctBranchId())
                .platformYieldPercentage(yield != null ? yield : 0.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDto.UserReliabilityResponse getUserRiskProfile(UUID userId) {
        // Return a default record if not found (avoids 404 for new users)
        UserReliabilityIndex entity = reliabilityRepository.findById(userId)
                .orElseGet(() -> UserReliabilityIndex.builder()
                        .userId(userId)
                        .reliabilityScore(50)
                        .totalBookings(0)
                        .noShowCount(0)
                        .lateCancelCount(0)
                        .riskLevel("STANDARD")
                        .build());
        return adminMapper.toResponse(entity);
    }

    // ---------- Branch Queries (with permission check) ----------

    // ---------- 1. Realtime occupancy ----------
    @Override
    public RestaurantDto.OccupancyResponse getRealtimeOccupancy(UUID branchId, UUID userId, String roles) {
        restaurantGrpcClient.validateBranchPermission(branchId, userId, roles);

        long occupied = Optional.ofNullable(redisTemplate.opsForValue().get("branch:occupancy:" + branchId))
                .map(Object::toString).map(Long::parseLong).orElse(0L);

        int total = restaurantGrpcClient.getBranchTotalTables(branchId);
        double rate = total > 0 ? (occupied * 100.0 / total) : 0;
        int wait = estimateWaitTime(occupied, total);
        return new RestaurantDto.OccupancyResponse(occupied, total, rate, wait);
    }

    // ---------- 2. Top‑selling items (use aggregated table) ----------
    @Override
    @Cacheable(value = "analytics", key = "'topItems:' + #branchId + ':' + #from + ':' + #to + ':' + #limit", unless = "#result.isEmpty()")
    public List<RestaurantDto.TopItemResponse> getTopItems(UUID branchId, LocalDate from, LocalDate to, int limit,
                                                           UUID userId, String roles) {
        restaurantGrpcClient.validateBranchPermission(branchId, userId, roles);

        GetTopItemsResponse response = orderGrpcClient.getTopItems(branchId, from, to, limit);
        // Collect item IDs
        List<String> itemIds = response.getItemsList().stream().map(TopItemPto::getItemId).toList();
        // Batch fetch menu details from menu service
        GetMenuDetailsResponse menuDetails = menuGrpcClient.getMenuDetails(itemIds);
        Map<String, String> nameMap = menuDetails.getItemsList().stream()
                .collect(Collectors.toMap(MenuItemDetailPto::getMenuItemId, MenuItemDetailPto::getName));
        return response.getItemsList().stream()
                .map(item -> new RestaurantDto.TopItemResponse(
                        UUID.fromString(item.getItemId()),
                        nameMap.getOrDefault(item.getItemId(), "Unknown"),
                        item.getQuantity(),
                        new BigDecimal(item.getRevenue())))
                .collect(Collectors.toList());
    }

    // ---------- 3. Revenue breakdown ----------
    @Override
    public RestaurantDto.RevenueBreakdownResponse getRevenueBreakdown(UUID branchId, String granularity,
                                                                      LocalDate date, UUID userId, String roles) {
        restaurantGrpcClient.validateBranchPermission(branchId, userId, roles);

        LocalDate target = date != null ? date : LocalDate.now();
        GetRevenueBreakdownResponse response = orderGrpcClient.getRevenueBreakdown(branchId, granularity, target);
        List<RestaurantDto.RevenueBreakdownResponse.RevenuePoint> points = response.getPointsList().stream()
                .map(p -> new RestaurantDto.RevenueBreakdownResponse.RevenuePoint(
                        p.getPeriod(),
                        new BigDecimal(p.getGmv()),
                        p.getOrders()))
                .collect(Collectors.toList());
        return new RestaurantDto.RevenueBreakdownResponse(
                granularity,
                points,
                new BigDecimal(response.getTotalGmv()),
                response.getTotalOrders());
    }

    // ---------- 4. Customer retention ----------
    @Override
    public RestaurantDto.RetentionResponse getCustomerRetention(UUID branchId, String period,
                                                                UUID userId, String roles) {
        restaurantGrpcClient.validateBranchPermission(branchId, userId, roles);
        GetCustomerRetentionResponse response = bookingGrpcClient.getCustomerRetention(branchId, period);
        return new RestaurantDto.RetentionResponse(
                response.getNewCustomers(),
                response.getRepeatCustomers(),
                response.getRepeatRate(),
                response.getAvgOrdersPerCustomer()
        );
    }

    // ---------- 5. POS sync health ----------
    @Override
    public RestaurantDto.PosHealthResponse getPosHealth(UUID branchId, UUID userId, String roles) {
        restaurantGrpcClient.validateBranchPermission(branchId, userId, roles);

        GetPosHealthResponse response = posGrpcClient.getPosHealth(branchId);
        OffsetDateTime lastSync = response.getLastSyncTime().isEmpty() ? null :
                OffsetDateTime.parse(response.getLastSyncTime());
        return new RestaurantDto.PosHealthResponse(
                lastSync,
                response.getSuccessRate(),
                response.getPendingOrders(),
                response.getAverageLatencyMs()
        );
    }

    // ---------- 6. Conversion funnel ----------
    @Override
    public RestaurantDto.FunnelResponse getConversionFunnel(UUID branchId, LocalDate startDate, LocalDate endDate,
                                                            UUID userId, String roles) {
        restaurantGrpcClient.validateBranchPermission(branchId, userId, roles);

        GetFunnelCountsResponse bookingResponse = bookingGrpcClient.getFunnelCounts(branchId, startDate, endDate);
        GetPaymentCountsResponse paymentResponse = paymentGrpcClient.getPaymentCounts(branchId, startDate, endDate);

        List<RestaurantDto.FunnelResponse.FunnelStage> stages = Arrays.asList(
                new RestaurantDto.FunnelResponse.FunnelStage("VIEWS", (int) bookingResponse.getViews()),
                new RestaurantDto.FunnelResponse.FunnelStage("BOOKINGS", (int) bookingResponse.getBookings()),
                new RestaurantDto.FunnelResponse.FunnelStage("PAYMENTS", (int) paymentResponse.getPayments()),
                new RestaurantDto.FunnelResponse.FunnelStage("ARRIVALS", (int) bookingResponse.getArrivals())
        );
        return new RestaurantDto.FunnelResponse(stages);
    }

    // ---------- 7. AOV trend ----------
    @Override
    public RestaurantDto.AovTrendResponse getAovTrend(UUID branchId, String granularity, int limit,
                                                      UUID userId, String roles) {
        restaurantGrpcClient.validateBranchPermission(branchId, userId, roles);
        List<RestaurantDto.AovTrendResponse.AovPoint> points;

        switch (granularity.toUpperCase()) {
            case "DAY":
                List<Object[]> dailyRows = financialLedgerRepository.findDailyAovNative(branchId, limit);
                points = dailyRows.stream()
                        .map(row -> new RestaurantDto.AovTrendResponse.AovPoint(
                                ((java.sql.Date) row[0]).toLocalDate().toString(),
                                (BigDecimal) row[1]))
                        .collect(Collectors.toList());
                break;
            case "WEEK":
                List<Object[]> weeklyRows = financialLedgerRepository.findWeeklyAovNative(branchId, limit);
                points = weeklyRows.stream()
                        .map(row -> new RestaurantDto.AovTrendResponse.AovPoint(
                                (String) row[0],   // period string like "2025-W19"
                                (BigDecimal) row[1]))
                        .collect(Collectors.toList());
                break;
            default:
                throw new InvalidRequestException("Unsupported granularity: " + granularity);
        }
        return new RestaurantDto.AovTrendResponse(granularity, points);
    }

    // ---------- 8. Top customers ----------
    @Override
    public List<RestaurantDto.TopCustomerResponse> getTopCustomers(UUID branchId, int limit,
                                                                   UUID userId, String roles) {
        restaurantGrpcClient.validateBranchPermission(branchId, userId, roles);

        // 1. Get top customers (user IDs, total spent, booking count) from booking service
        GetTopCustomersResponse response = bookingGrpcClient.getTopCustomers(branchId, limit);

        // 2. Batch fetch user names from user service
        List<String> userIds = response.getCustomersList().stream()
                .map(TopCustomerPto::getUserId)
                .toList();
        GetUserDetailsResponse users = userGrpcClient.getUserDetails(GetUserDetailsRequest.newBuilder()
                .addAllUserIds(userIds)
                .build());
        Map<String, String> nameMap = users.getProfilesList().stream()
                .collect(Collectors.toMap(GetUserDetailResponse::getUserId, GetUserDetailResponse::getFullName));

        // 3. Build response
        return response.getCustomersList().stream()
                .map(c -> new RestaurantDto.TopCustomerResponse(
                        UUID.fromString(c.getUserId()),
                        nameMap.getOrDefault(c.getUserId(), "Unknown"),
                        BigDecimal.valueOf(c.getTotalSpent()),
                        c.getBookingCount()))
                .collect(Collectors.toList());
    }

    // ---------- 9. Menu performance ----------
    @Override
    public RestaurantDto.MenuPerformanceResponse getMenuPerformance(UUID branchId, LocalDate startDate, LocalDate endDate,
                                                                    String groupBy, UUID userId, String roles) {
        restaurantGrpcClient.validateBranchPermission(branchId, userId, roles);

        // 1. Fetch item‑level performance from order service
        GetMenuPerformanceResponse itemResponse = orderGrpcClient.getMenuPerformance(branchId, startDate, endDate, "ITEM");
        List<MenuPerformancePoint> itemPoints = itemResponse.getPointsList();

        if (itemPoints.isEmpty()) {
            return new RestaurantDto.MenuPerformanceResponse(groupBy, List.of());
        }

        // 2. Batch fetch item details (name, category) from menu service
        List<String> itemIds = itemPoints.stream().map(MenuPerformancePoint::getId).toList();
        GetMenuDetailsResponse menuDetails = menuGrpcClient.getMenuDetails(itemIds);
        Map<String, MenuItemDetailPto> itemMap = menuDetails.getItemsList().stream()
                .collect(Collectors.toMap(MenuItemDetailPto::getMenuItemId, i -> i));

        if ("ITEM".equalsIgnoreCase(groupBy)) {
            // Build response directly with names
            List<RestaurantDto.MenuPerformanceResponse.MenuPerformancePoint> points = itemPoints.stream()
                    .map(p -> {
                        MenuItemDetailPto item = itemMap.get(p.getId());
                        if (item == null) return null;
                        return new RestaurantDto.MenuPerformanceResponse.MenuPerformancePoint(
                                UUID.fromString(p.getId()),
                                item.getName(),
                                new BigDecimal(p.getRevenue()),
                                p.getQuantity());
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return new RestaurantDto.MenuPerformanceResponse(groupBy, points);
        } else if ("CATEGORY".equalsIgnoreCase(groupBy)) {
            // Group by category
            Map<String, CategoryAggregate> categoryMap = new LinkedHashMap<>();
            for (MenuPerformancePoint p : itemPoints) {
                MenuItemDetailPto item = itemMap.get(p.getId());
                if (item == null) continue;
                String catId = item.getCategoryId();
                String catName = item.getCategoryName();
                CategoryAggregate agg = categoryMap.computeIfAbsent(catId,
                        k -> new CategoryAggregate(catId, catName, BigDecimal.ZERO, 0));
                agg.revenue = agg.revenue.add(new BigDecimal(p.getRevenue()));
                agg.quantity += p.getQuantity();
            }
            List<RestaurantDto.MenuPerformanceResponse.MenuPerformancePoint> points = categoryMap.values().stream()
                    .map(agg -> new RestaurantDto.MenuPerformanceResponse.MenuPerformancePoint(
                            UUID.fromString(agg.id), agg.name, agg.revenue, agg.quantity))
                    .collect(Collectors.toList());
            return new RestaurantDto.MenuPerformanceResponse(groupBy, points);
        }
        throw new InvalidRequestException("Invalid groupBy: " + groupBy);
    }

    // Helper record
    private static class CategoryAggregate {
        String id, name;
        BigDecimal revenue;
        int quantity;

        CategoryAggregate(String id, String name, BigDecimal revenue, int quantity) {
            this.id = id;
            this.name = name;
            this.revenue = revenue;
            this.quantity = quantity;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserReliabilityIndex getUserReliability(UUID userId) {
        return reliabilityRepository.findById(userId).orElse(null);
    }

    // ---------- Event Processing (Transactional, with type safety) ----------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processEvent(String source, String type, UUID branchId, Object payload) {
        // 1. Persist raw event to system_events (audit log)
        SystemEvent eventLog = SystemEvent.builder()
                .serviceSource(source)
                .eventType(type)
                .branchId(branchId)
                .payload(objectMapper.valueToTree(payload))
                .occurredAt(OffsetDateTime.now())
                .build();
        systemEventsRepository.save(eventLog);

        // 2. Route to specific handlers with safe casting
        try {
            switch (source) {
                case SOURCE_BOOKING_SERVICE -> handleBookingEvent(type, branchId, payload);
                case SOURCE_PAYMENT_SERVICE -> handlePaymentEvent(type, branchId, payload);
                case SOURCE_POS_SERVICE -> handlePosEvent(type, branchId, payload);
                case SOURCE_REVIEW_SERVICE -> handleReviewEvent(type, branchId, payload);
                default -> log.debug("Unknown service source: {}, event logged only", source);
            }
        } catch (ClassCastException e) {
            log.error("Invalid payload type for event {}.{}: expected a specific event class, got {}",
                    source, type, payload.getClass().getSimpleName(), e);
        } catch (Exception e) {
            log.error("Error processing event {}.{}: {}", source, type, e.getMessage(), e);
            // Re-throw to rollback transaction
            throw e;
        }
    }

    private void handleBookingEvent(String type, UUID branchId, Object payload) {
        if (!(payload instanceof BookingEvent.BookingStatusUpdatedEvent event)) {
            log.warn("Booking event payload is not BookingStatusUpdatedEvent: {}", payload.getClass());
            return;
        }
        switch (type) {
            case EVENT_BOOKING_CREATED -> handleOccupancyChange(branchId, event, true);
            case EVENT_BOOKING_CANCELED -> handleOccupancyChange(branchId, event, false);
            case EVENT_BOOKING_COMPLETED, EVENT_BOOKING_NO_SHOW -> handleReliabilityUpdate(type, event);
            default -> log.debug("Booking event type {} logged, no further processing", type);
        }
    }

    private void handlePaymentEvent(String type, UUID branchId, Object payload) {
        if (!(payload instanceof PaymentEvent.PaymentCompletedEvent event)) {
            log.warn("Payment event payload is not PaymentCompletedEvent: {}", payload.getClass());
            return;
        }
        if (EVENT_PAYMENT_SUCCESS.equals(type)) {
            handleFinancialClearing(branchId, event);
        } else {
            log.debug("Payment event type {} logged, no further processing", type);
        }
    }

    private void handlePosEvent(String type, UUID branchId, Object payload) {
        // Currently only logging; add business logic later if needed
        log.debug("POS event type {} logged", type);
    }

    private void handleReviewEvent(String type, UUID branchId, Object payload) {
        // Currently only logging; add business logic later if needed
        log.debug("Review event type {} logged", type);
    }

    // ---------- Occupancy & Yield Management ----------
    private void handleOccupancyChange(UUID branchId, BookingEvent.BookingStatusUpdatedEvent event, boolean isAdding) {
        OffsetDateTime bookingTime = event.bookingTime();
        // Round down to nearest 15‑minute slot
        OffsetDateTime slotTime = bookingTime.withMinute((bookingTime.getMinute() / 15) * 15)
                .withSecond(0).withNano(0);

        BranchYieldManagement.BranchSlotId slotId = new BranchYieldManagement.BranchSlotId(branchId, slotTime);

        // Retry logic for optimistic locking (if @Version is used)
        int retries = 3;
        while (retries-- > 0) {
            try {
                BranchYieldManagement stat = yieldRepository.findById(slotId)
                        .orElseGet(() -> createNewSlot(branchId, slotTime));

                int adjustment = isAdding ? 1 : -1;
                stat.setCurrentBookings(Math.max(0, stat.getCurrentBookings() + adjustment));
                if (stat.getTotalCapacity() > 0) {
                    BigDecimal rate = BigDecimal.valueOf(stat.getCurrentBookings())
                            .divide(BigDecimal.valueOf(stat.getTotalCapacity()), 2, RoundingMode.HALF_UP);
                    stat.setOccupancyRate(rate);
                    applyYieldStrategy(stat);
                }
                yieldRepository.save(stat);
                log.info("Branch {} slot {} updated: bookings={}, rate={}%, demand={}",
                        branchId, slotTime, stat.getCurrentBookings(),
                        stat.getOccupancyRate().multiply(BigDecimal.valueOf(100)),
                        stat.getDemandLevel());
                break; // success
            } catch (OptimisticLockingFailureException e) {
                if (retries == 0) throw e;
                log.warn("Optimistic lock failure for slot {}, retrying...", slotId);
            }
        }
    }

    private void applyYieldStrategy(BranchYieldManagement stats) {
        double rate = stats.getOccupancyRate().doubleValue();
        if (rate < 0.30) {
            stats.setDemandLevel("LOW");
            stats.setRecommendedDiscountPct(0.40f);   // 40% discount
        } else if (rate < 0.70) {
            stats.setDemandLevel("MEDIUM");
            stats.setRecommendedDiscountPct(0.20f);   // 20% discount
        } else {
            stats.setDemandLevel("HIGH");
            stats.setRecommendedDiscountPct(0.0f);     // no discount
        }
    }

    private BranchYieldManagement createNewSlot(UUID branchId, OffsetDateTime slotTime) {
        // Fetch real capacity from restaurant service (implement gRPC method)
        int branchCapacity = getBranchCapacity(branchId);
        return BranchYieldManagement.builder()
                .id(new BranchYieldManagement.BranchSlotId(branchId, slotTime))
                .totalCapacity(branchCapacity)
                .currentBookings(0)
                .occupancyRate(BigDecimal.ZERO)
                .demandLevel("LOW")
                .recommendedDiscountPct(0.40f)
                .build();
    }

    private int getBranchCapacity(UUID branchId) {
        // TODO: Implement gRPC call to restaurant service to fetch actual table count for this branch
        // For now return a default; replace with:
        // return restaurantGrpcClient.getBranchTableCapacity(branchId);
        log.debug("Branch capacity not yet fetched via gRPC, using default 50");
        return 50;
    }

    // ---------- User Reliability ----------
    private void handleReliabilityUpdate(String type, BookingEvent.BookingStatusUpdatedEvent event) {
        UUID userId = event.userId();
        if (userId == null) {
            log.warn("Reliability update skipped: no userId in event");
            return;
        }

        UserReliabilityIndex stats = reliabilityRepository.findById(userId)
                .orElseGet(() -> UserReliabilityIndex.builder()
                        .userId(userId)
                        .reliabilityScore(50)
                        .totalBookings(0)
                        .noShowCount(0)
                        .lateCancelCount(0)
                        .riskLevel("STANDARD")
                        .build());

        stats.setTotalBookings(stats.getTotalBookings() + 1);
        switch (type) {
            case EVENT_BOOKING_COMPLETED -> {
                stats.setReliabilityScore(Math.min(100, stats.getReliabilityScore() + 2));
                log.debug("User {} completed booking. Score +2.", userId);
            }
            case EVENT_BOOKING_NO_SHOW -> {
                stats.setNoShowCount(stats.getNoShowCount() + 1);
                stats.setReliabilityScore(Math.max(0, stats.getReliabilityScore() - 20));
                log.warn("User {} marked NO_SHOW. Score -20.", userId);
            }
            case EVENT_BOOKING_CANCELED -> {
                stats.setLateCancelCount(stats.getLateCancelCount() + 1);
                stats.setReliabilityScore(Math.max(0, stats.getReliabilityScore() - 5));
                log.info("User {} late cancelled. Score -5.", userId);
            }
            default -> {
            }
        }

        // Update risk level
        if (stats.getReliabilityScore() < 20) {
            stats.setRiskLevel("CRITICAL");
        } else if (stats.getReliabilityScore() < 40) {
            stats.setRiskLevel("HIGH_RISK");
        } else {
            stats.setRiskLevel("STANDARD");
        }
        reliabilityRepository.save(stats);
    }

    // ---------- Financial Clearing ----------
    private void handleFinancialClearing(UUID branchId, PaymentEvent.PaymentCompletedEvent event) {
        BigDecimal amount = event.amount();
        BigDecimal commissionRate = event.commissionRate(); // e.g., 0.10 for 10%
        LocalDate reportDate = LocalDate.now();

        FinancialLedger ledger = financialLedgerRepository.findByBranchIdAndReportDate(branchId, reportDate)
                .orElseGet(() -> FinancialLedger.builder()
                        .branchId(branchId)
                        .reportDate(reportDate)
                        .totalGmv(BigDecimal.ZERO)
                        .commissionEarned(BigDecimal.ZERO)
                        .payoutDueRestaurant(BigDecimal.ZERO)
                        .refundVolume(BigDecimal.ZERO)
                        .build());

        BigDecimal commission = amount.multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPayout = amount.subtract(commission);

        ledger.setTotalGmv(ledger.getTotalGmv().add(amount));
        ledger.setCommissionEarned(ledger.getCommissionEarned().add(commission));
        ledger.setPayoutDueRestaurant(ledger.getPayoutDueRestaurant().add(netPayout));
        financialLedgerRepository.save(ledger);

        log.info("Financials updated for branch {}: GMV +{}, Commission +{}, Net +{}",
                branchId, amount, commission, netPayout);
    }

    private int estimateWaitTime(long occupied, int total) {
        if (occupied < total * 0.8) return 0;
        return (int) ((occupied - total * 0.8) / (total * 0.01) * 5); // simple logic
    }

    private LocalDate getStartDateForPeriod(String period) {
        return switch (period.toUpperCase()) {
            case "WEEK" -> LocalDate.now().minusWeeks(1);
            case "MONTH" -> LocalDate.now().minusMonths(1);
            case "QUARTER" -> LocalDate.now().minusMonths(3);
            default -> LocalDate.now().minusMonths(1);
        };
    }
}