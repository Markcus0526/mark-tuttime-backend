package com.tuttime.user.service;

import com.tuttime.user.dto.CreateUserReportRequest;
import com.tuttime.user.dto.UserReportResponse;

import java.util.List;

/**
 * Service for managing user reports
 */
public interface UserReportService {
    
    /**
     * Create a report about a user
     * 
     * @param reporterUserId User ID who is reporting
     * @param reportedUserId User ID being reported
     * @param request Report request with category, reason, and description
     * @return Created report response
     */
    UserReportResponse createReport(Long reporterUserId, Long reportedUserId, CreateUserReportRequest request);
    
    /**
     * Get list of reports submitted by a user
     * 
     * @param userId User ID
     * @return List of reports
     */
    List<UserReportResponse> getMyReports(Long userId);
    
    /**
     * Get report by ID (only accessible by reporter)
     * 
     * @param reportId Report ID
     * @param userId User ID requesting the report
     * @return Report response
     */
    UserReportResponse getReportById(Long reportId, Long userId);
}





