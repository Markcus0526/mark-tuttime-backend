package com.tuttime.analytics.controller;

import com.tuttime.analytics.dto.RestaurantDto;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/restaurant")
@Tag(name = "2.Restaurant Analytics", description = "Endpoints for restaurant owners to monitor branch performance")
@RequiredArgsConstructor
public class RestaurantController {

    private final AnalyticsService analyticsService;

    // 1. Real‑time occupancy
    @GetMapping("/{branchId}/realtime-occupancy")
    @Operation(summary = "Current occupancy and estimated wait time")
    public ResponseEntity<ApiRes<RestaurantDto.OccupancyResponse>> getRealtimeOccupancy(
            @PathVariable UUID branchId) {
        validateAdminOrOwner();
        UUID userId = UuidUtil.toUuid(UserContext.getUserId());
        String roles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                analyticsService.getRealtimeOccupancy(branchId, userId, roles)));
    }

    // 2. Top‑selling items
    @GetMapping("/{branchId}/top-items")
    @Operation(summary = "Most popular dishes in a date range")
    public ResponseEntity<ApiRes<List<RestaurantDto.TopItemResponse>>> getTopItems(
            @PathVariable UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        validateAdminOrOwner();
        UUID userId = UuidUtil.toUuid(UserContext.getUserId());
        String roles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                analyticsService.getTopItems(branchId, from, to, limit, userId, roles)));
    }

    // 3. Revenue breakdown
    @GetMapping("/{branchId}/revenue-breakdown")
    @Operation(summary = "Revenue grouped by hour/day/week/month")
    public ResponseEntity<ApiRes<RestaurantDto.RevenueBreakdownResponse>> getRevenueBreakdown(
            @PathVariable UUID branchId,
            @RequestParam String granularity,  // HOUR, DAY, WEEK, MONTH
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        validateAdminOrOwner();
        UUID userId = UuidUtil.toUuid(UserContext.getUserId());
        String roles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                analyticsService.getRevenueBreakdown(branchId, granularity, date, userId, roles)));
    }

    // 4. Customer retention
    @GetMapping("/{branchId}/customer-retention")
    @Operation(summary = "New vs repeat customers")
    public ResponseEntity<ApiRes<RestaurantDto.RetentionResponse>> getCustomerRetention(
            @PathVariable UUID branchId,
            @RequestParam(defaultValue = "MONTH") String period) { // WEEK, MONTH, QUARTER
        validateAdminOrOwner();
        UUID userId = UuidUtil.toUuid(UserContext.getUserId());
        String roles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                analyticsService.getCustomerRetention(branchId, period, userId, roles)));
    }

    // 5. POS sync health
    @GetMapping("/{branchId}/pos-health")
    @Operation(summary = "POS integration health metrics")
    public ResponseEntity<ApiRes<RestaurantDto.PosHealthResponse>> getPosHealth(
            @PathVariable UUID branchId) {
        validateAdminOrOwner();
        UUID userId = UuidUtil.toUuid(UserContext.getUserId());
        String roles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                analyticsService.getPosHealth(branchId, userId, roles)));
    }

    // 6. Conversion funnel
    @GetMapping("/{branchId}/conversion-funnel")
    @Operation(summary = "Booking to arrival conversion rates")
    public ResponseEntity<ApiRes<RestaurantDto.FunnelResponse>> getConversionFunnel(
            @PathVariable UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateAdminOrOwner();
        UUID userId = UuidUtil.toUuid(UserContext.getUserId());
        String roles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                analyticsService.getConversionFunnel(branchId, startDate, endDate, userId, roles)));
    }

    // 7. AOV trend
    @GetMapping("/{branchId}/aov-trend")
    @Operation(summary = "Average order value over time")
    public ResponseEntity<ApiRes<RestaurantDto.AovTrendResponse>> getAovTrend(
            @PathVariable UUID branchId,
            @RequestParam(defaultValue = "DAY") String granularity,
            @RequestParam(defaultValue = "30") int limit) {
        validateAdminOrOwner();
        UUID userId = UuidUtil.toUuid(UserContext.getUserId());
        String roles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                analyticsService.getAovTrend(branchId, granularity, limit, userId, roles)));
    }

    // 8. Top customers
    @GetMapping("/{branchId}/top-customers")
    @Operation(summary = "Most valuable customers")
    public ResponseEntity<ApiRes<List<RestaurantDto.TopCustomerResponse>>> getTopCustomers(
            @PathVariable UUID branchId,
            @RequestParam(defaultValue = "10") int limit) {
        validateAdminOrOwner();
        UUID userId = UuidUtil.toUuid(UserContext.getUserId());
        String roles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                analyticsService.getTopCustomers(branchId, limit, userId, roles)));
    }

    // 9. Menu performance
    @GetMapping("/{branchId}/menu-performance")
    @Operation(summary = "Revenue per category or per item")
    public ResponseEntity<ApiRes<RestaurantDto.MenuPerformanceResponse>> getMenuPerformance(
            @PathVariable UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "CATEGORY") String groupBy) {
        validateAdminOrOwner();
        UUID userId = UuidUtil.toUuid(UserContext.getUserId());
        String roles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                analyticsService.getMenuPerformance(branchId, startDate, endDate, groupBy, userId, roles)));
    }

    // ------------- helper methods -------------
    private void validateLogin() {
        if (!UserContext.isValidUUID()) {
            throw new UnauthorizedException("Unauthorized: You should login");
        }
    }

    private void validateAdmin() {
        if (!UserContext.isValidUUID() || !UserContext.isRoleAdmin()) {
            throw new ForbiddenException("Access forbidden: Insufficient permissions");
        }
    }

    private void validateAdminOrOwner() {
        if (!UserContext.isValidUUID()
                || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
            throw new ForbiddenException("Access forbidden: Insufficient permissions");
        }
    }

    private void validateAdminOrOwnerOrManager() {
        if (!UserContext.isValidUUID()
                || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
            throw new ForbiddenException("Access forbidden: Insufficient permissions");
        }
    }
}