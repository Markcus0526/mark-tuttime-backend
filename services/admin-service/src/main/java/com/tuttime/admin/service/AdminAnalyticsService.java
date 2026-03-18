package com.tuttime.admin.service;

import com.tuttime.admin.dto.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin Analytics Service Interface
 */
public interface AdminAnalyticsService {
    DashboardStatsResponse getDashboardStats();
    UserStatsResponse getUserStats(LocalDate startDate, LocalDate endDate);
    MiningStatsResponse getMiningStats(LocalDate startDate, LocalDate endDate);
    TokenStatsResponse getTokenStats();
    ConversionStatsResponse getConversionStats();
    
    // New methods for enhanced dashboard
    List<HistoricalDataPoint> getApGrowthHistory(Period period);
    List<HistoricalDataPoint> getApartGrowthHistory(Period period);
    ConversionSummaryResponse getLastConversionSummary();
    
    // Registration statistics
    RegistrationStatsResponse getRegistrationsPerDay(LocalDate startDate, LocalDate endDate);
    RegistrationStatsResponse getRegistrationsPerMonth(LocalDate startDate, LocalDate endDate);
    
    // Task completion statistics
    TaskCompletionStatsResponse getPopularTasksPerDay(LocalDate startDate, LocalDate endDate);
    TaskCompletionStatsResponse getPopularTasksPerMonth(LocalDate startDate, LocalDate endDate);
}

