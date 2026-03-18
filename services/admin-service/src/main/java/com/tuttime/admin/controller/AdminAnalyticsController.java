package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminAnalyticsService;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminLeaderboardService;
import com.tuttime.admin.util.PeriodConverter;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Admin Analytics Controller
 * 
 * <p>Provides analytics and dashboard data for admin panel.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin Analytics", description = "Analytics and dashboard APIs")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;
    private final AdminAuditService auditService;
    private final AdminLeaderboardService leaderboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get dashboard stats", description = "Get overall dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(
            @AuthenticationPrincipal Long adminId) {
        
        long startTime = System.currentTimeMillis();
        DashboardStatsResponse stats = analyticsService.getDashboardStats();
        long duration = System.currentTimeMillis() - startTime;
        
        // Alert: Request latency > 1 second
        if (duration > 1000) {
            log.warn("ALERT: Slow dashboard request - adminId={}, duration={}ms", adminId, duration);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully", stats));
    }

    @GetMapping("/users/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user statistics", description = "Get user statistics and growth metrics")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal Long adminId) {
        
        UserStatsResponse stats = analyticsService.getUserStats(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("User stats retrieved successfully", stats));
    }

    @GetMapping("/mining/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get mining statistics", description = "Get AP Credits mining statistics")
    public ResponseEntity<ApiResponse<MiningStatsResponse>> getMiningStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal Long adminId) {
        
        MiningStatsResponse stats = analyticsService.getMiningStats(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Mining stats retrieved successfully", stats));
    }

    @GetMapping("/tokens/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get token statistics", description = "Get APART token statistics")
    public ResponseEntity<ApiResponse<TokenStatsResponse>> getTokenStats(
            @AuthenticationPrincipal Long adminId) {
        
        TokenStatsResponse stats = analyticsService.getTokenStats();
        return ResponseEntity.ok(ApiResponse.success("Token stats retrieved successfully", stats));
    }

    @GetMapping("/conversions/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get conversion statistics", description = "Get AP to APART conversion statistics")
    public ResponseEntity<ApiResponse<ConversionStatsResponse>> getConversionStats(
            @AuthenticationPrincipal Long adminId) {
        
        ConversionStatsResponse stats = analyticsService.getConversionStats();
        return ResponseEntity.ok(ApiResponse.success("Conversion stats retrieved successfully", stats));
    }
    
    @GetMapping("/dashboard/ap-growth-history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get AP growth history", description = "Get historical AP Credits growth data for charts")
    public ResponseEntity<ApiResponse<java.util.List<HistoricalDataPoint>>> getApGrowthHistory(
            @RequestParam(required = false, defaultValue = "total") String period,
            @AuthenticationPrincipal Long adminId) {
        
        Period periodEnum = PeriodConverter.fromString(period);
        java.util.List<HistoricalDataPoint> history = analyticsService.getApGrowthHistory(periodEnum);
        return ResponseEntity.ok(ApiResponse.success("AP growth history retrieved successfully", history));
    }
    
    @GetMapping("/dashboard/apart-growth-history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get APART growth history", description = "Get historical APART token growth data for charts")
    public ResponseEntity<ApiResponse<java.util.List<HistoricalDataPoint>>> getApartGrowthHistory(
            @RequestParam(required = false, defaultValue = "total") String period,
            @AuthenticationPrincipal Long adminId) {
        
        Period periodEnum = PeriodConverter.fromString(period);
        java.util.List<HistoricalDataPoint> history = analyticsService.getApartGrowthHistory(periodEnum);
        return ResponseEntity.ok(ApiResponse.success("APART growth history retrieved successfully", history));
    }
    
    @GetMapping("/conversions/last-summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get last conversion summary", description = "Get summary statistics for the most recent conversion event")
    public ResponseEntity<ApiResponse<ConversionSummaryResponse>> getLastConversionSummary(
            @AuthenticationPrincipal Long adminId) {
        
        ConversionSummaryResponse summary = analyticsService.getLastConversionSummary();
        return ResponseEntity.ok(ApiResponse.success("Last conversion summary retrieved successfully", summary));
    }

    // ==================== Leaderboard Endpoints ====================

    @GetMapping("/leaderboards/top-ap-users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get top users by AP Credits", description = "Get ranked list of users by AP Credits balance")
    public ResponseEntity<ApiResponse<java.util.List<TopUserResponse>>> getTopUsersByApCredits(
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) String period,
            @AuthenticationPrincipal Long adminId) {
        
        java.util.List<TopUserResponse> users = leaderboardService.getTopUsersByApCredits(limit, period);
        return ResponseEntity.ok(ApiResponse.success("Top users by AP Credits retrieved successfully", users));
    }

    @GetMapping("/leaderboards/top-apart-users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get top users by APART balance", description = "Get ranked list of users by APART token balance")
    public ResponseEntity<ApiResponse<java.util.List<TopUserResponse>>> getTopUsersByApartBalance(
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) String period,
            @AuthenticationPrincipal Long adminId) {
        
        java.util.List<TopUserResponse> users = leaderboardService.getTopUsersByApartBalance(limit, period);
        return ResponseEntity.ok(ApiResponse.success("Top users by APART balance retrieved successfully", users));
    }

    @GetMapping("/leaderboards/top-stakings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get top staking positions", description = "Get ranked staking positions sortable by APY or mining bonus")
    public ResponseEntity<ApiResponse<java.util.List<TopStakingResponse>>> getTopStakings(
            @RequestParam(defaultValue = "apy") String sortBy,
            @RequestParam(required = false) Long poolId,
            @RequestParam(defaultValue = "100") Integer limit,
            @AuthenticationPrincipal Long adminId) {
        
        java.util.List<TopStakingResponse> stakings = leaderboardService.getTopStakings(sortBy, poolId, limit);
        return ResponseEntity.ok(ApiResponse.success("Top stakings retrieved successfully", stakings));
    }

    // ==================== Geolocation Analytics Endpoints ====================

    @GetMapping("/geolocation/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get geolocation statistics", description = "Get per country/city statistics including users, miners, AP speed, APART reserve, conversion amounts, and staked amounts")
    public ResponseEntity<ApiResponse<java.util.List<GeolocationStatsResponse>>> getGeolocationStats(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @AuthenticationPrincipal Long adminId) {
        
        // URL-decode parameters (Spring should do this automatically, but handle it manually to be safe)
        // Only decode if the string contains URL-encoded characters (%)
        String decodedCountry = country;
        String decodedCity = city;
        try {
            if (country != null && !country.isEmpty() && country.contains("%")) {
                decodedCountry = java.net.URLDecoder.decode(country, java.nio.charset.StandardCharsets.UTF_8);
                log.debug("URL-decoded country: '{}' -> '{}'", country, decodedCountry);
            }
            if (city != null && !city.isEmpty() && city.contains("%")) {
                decodedCity = java.net.URLDecoder.decode(city, java.nio.charset.StandardCharsets.UTF_8);
                log.debug("URL-decoded city: '{}' -> '{}'", city, decodedCity);
            }
        } catch (Exception e) {
            // If decoding fails, use original values
            log.warn("Failed to URL-decode geolocation parameters, using original values: {}", e.getMessage());
        }
        
        java.util.List<GeolocationStatsResponse> stats = leaderboardService.getGeolocationStats(decodedCountry, decodedCity);
        return ResponseEntity.ok(ApiResponse.success("Geolocation statistics retrieved successfully", stats));
    }

    // ==================== Registration Statistics Endpoints ====================

    @GetMapping("/registrations/daily")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get registrations per day", description = "Get daily user registration statistics")
    public ResponseEntity<ApiResponse<RegistrationStatsResponse>> getRegistrationsPerDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal Long adminId) {
        
        RegistrationStatsResponse stats = analyticsService.getRegistrationsPerDay(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Registrations per day retrieved successfully", stats));
    }

    @GetMapping("/registrations/monthly")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get registrations per month", description = "Get monthly user registration statistics")
    public ResponseEntity<ApiResponse<RegistrationStatsResponse>> getRegistrationsPerMonth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal Long adminId) {
        
        RegistrationStatsResponse stats = analyticsService.getRegistrationsPerMonth(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Registrations per month retrieved successfully", stats));
    }

    // ==================== Task Completion Statistics Endpoints ====================

    @GetMapping("/tasks/popular/daily")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get popular tasks per day", description = "Get the most popular tasks completed by users per day")
    public ResponseEntity<ApiResponse<TaskCompletionStatsResponse>> getPopularTasksPerDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal Long adminId) {
        
        TaskCompletionStatsResponse stats = analyticsService.getPopularTasksPerDay(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Popular tasks per day retrieved successfully", stats));
    }

    @GetMapping("/tasks/popular/monthly")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get popular tasks per month", description = "Get the most popular tasks completed by users per month")
    public ResponseEntity<ApiResponse<TaskCompletionStatsResponse>> getPopularTasksPerMonth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal Long adminId) {
        
        TaskCompletionStatsResponse stats = analyticsService.getPopularTasksPerMonth(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Popular tasks per month retrieved successfully", stats));
    }
}

