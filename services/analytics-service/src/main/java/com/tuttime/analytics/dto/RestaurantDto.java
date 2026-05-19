package com.tuttime.analytics.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class RestaurantDto {

    // Realtime Occupancy
    public record OccupancyResponse(
            long occupiedTables,
            int totalTables,
            double occupancyRate,
            int estimatedWaitTimeMinutes
    ) {
    }

    // Top‑selling items
    public record TopItemResponse(
            UUID itemId,
            String itemName,
            int quantitySold,
            BigDecimal revenue
    ) {
    }

    // Revenue breakdown
    public record RevenueBreakdownResponse(
            String granularity,  // HOUR, DAY, WEEK, MONTH
            List<RevenuePoint> data,
            BigDecimal totalGmv,
            int totalOrders
    ) {
        public record RevenuePoint(String period, BigDecimal gmv, int orders) {
        }
    }

    // Customer retention
    public record RetentionResponse(
            int newCustomers,
            int repeatCustomers,
            double repeatRate,
            double averageOrdersPerCustomer
    ) {
    }

    // POS sync health
    public record PosHealthResponse(
            OffsetDateTime lastSyncTime,
            double syncSuccessRate,
            int pendingOrders,
            double averageLatencyMs
    ) {
    }

    // Conversion funnel
    public record FunnelResponse(
            List<FunnelStage> stages
    ) {
        public record FunnelStage(String stage, long count) {
        }
    }

    // AOV trend
    public record AovTrendResponse(
            String granularity,
            List<AovPoint> data
    ) {
        public record AovPoint(String period, BigDecimal aov) {
        }
    }

    // Top customers
    public record TopCustomerResponse(
            UUID userId,
            String fullName,
            BigDecimal totalSpent,
            int bookingCount
    ) {
    }

    // Menu performance
    public record MenuPerformanceResponse(
            String groupBy,  // CATEGORY or ITEM
            List<MenuPerformancePoint> data
    ) {
        public record MenuPerformancePoint(
                UUID id,          // categoryId or itemId
                String name,
                BigDecimal revenue,
                int quantity
        ) {
        }
    }

    @Builder
    public record YieldHeatmapResponse(
            UUID branchId,
            OffsetDateTime timeSlot,
            Integer currentBookings,
            BigDecimal occupancyRate,
            String demandLevel,
            Float recommendedDiscountPct
    ) {
    }

    @Builder
    public record DailyLedgerResponse(
            UUID branchId,
            LocalDate reportDate,
            BigDecimal totalGmv,
            BigDecimal commissionEarned,
            BigDecimal payoutDueRestaurant,
            BigDecimal refundVolume
    ) {
    }

    public record BranchPerformanceResponse(
            UUID branchId,
            Integer avgPrepTimeSec,
            BigDecimal posSyncHealthPct,
            BigDecimal avgGuestRating,
            UUID popularDishId
    ) {
    }
}
