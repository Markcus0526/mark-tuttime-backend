package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.CreateUserReportRequest;
import com.tuttime.user.dto.UserReportResponse;
import com.tuttime.user.entity.User;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.service.UserReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for managing user reports using Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserReportServiceImpl implements UserReportService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    
    private static final String REPORT_KEY_PREFIX = "report:";
    private static final String REPORT_REPORTER_KEY_PREFIX = "report:reporter:";
    private static final String REPORT_REPORTED_KEY_PREFIX = "report:reported:";
    private static final String REPORT_NEXT_ID_KEY = "report:next_id";
    private static final String REPORT_DUPLICATE_KEY_PREFIX = "report:duplicate:";
    // Optional TTL - set to null for no expiration
    private static final Long REPORT_TTL_SECONDS = null; // null = no expiration, or set to Duration.ofDays(180).getSeconds()
    
    @Override
    public UserReportResponse createReport(Long reporterUserId, Long reportedUserId, CreateUserReportRequest request) {
        // Validate users
        if (reporterUserId.equals(reportedUserId)) {
            throw new BusinessException("Cannot report yourself");
        }
        
        userRepository.findByIdAndNotDeleted(reporterUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Reporter user not found"));
        User reportedUser = userRepository.findByIdAndNotDeleted(reportedUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Reported user not found"));
        
        // Check for duplicate reports (prevent multiple reports for same user)
        String duplicateKey = REPORT_DUPLICATE_KEY_PREFIX + reporterUserId + ":" + reportedUserId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(duplicateKey))) {
            throw new BusinessException("You have already submitted a report for this user");
        }
        
        // Generate report ID
        Long reportId = redisTemplate.opsForValue().increment(REPORT_NEXT_ID_KEY);
        if (reportId == null) {
            reportId = 1L;
            redisTemplate.opsForValue().set(REPORT_NEXT_ID_KEY, "1");
        }
        
        // Create report hash
        String reportKey = REPORT_KEY_PREFIX + reportId;
        Map<String, String> reportData = new HashMap<>();
        reportData.put("report_id", reportId.toString());
        reportData.put("reporter_user_id", reporterUserId.toString());
        reportData.put("reported_user_id", reportedUserId.toString());
        reportData.put("report_category", request.getReportCategory());
        reportData.put("report_reason", request.getReportReason());
        reportData.put("report_description", request.getReportDescription() != null ? request.getReportDescription() : "");
        reportData.put("created_at", OffsetDateTime.now(ZoneOffset.UTC).toString());
        
        redisTemplate.opsForHash().putAll(reportKey, reportData);
        if (REPORT_TTL_SECONDS != null) {
            redisTemplate.expire(reportKey, java.time.Duration.ofSeconds(REPORT_TTL_SECONDS));
        }
        
        // Add to reporter's set
        String reporterKey = REPORT_REPORTER_KEY_PREFIX + reporterUserId;
        redisTemplate.opsForSet().add(reporterKey, reportId.toString());
        if (REPORT_TTL_SECONDS != null) {
            redisTemplate.expire(reporterKey, java.time.Duration.ofSeconds(REPORT_TTL_SECONDS));
        }
        
        // Add to reported user's set
        String reportedKey = REPORT_REPORTED_KEY_PREFIX + reportedUserId;
        redisTemplate.opsForSet().add(reportedKey, reportId.toString());
        if (REPORT_TTL_SECONDS != null) {
            redisTemplate.expire(reportedKey, java.time.Duration.ofSeconds(REPORT_TTL_SECONDS));
        }
        
        // Add to duplicate check set
        redisTemplate.opsForSet().add(duplicateKey, reportId.toString());
        if (REPORT_TTL_SECONDS != null) {
            redisTemplate.expire(duplicateKey, java.time.Duration.ofSeconds(REPORT_TTL_SECONDS));
        }
        
        log.info("User {} reported user {} - Report ID: {}", reporterUserId, reportedUserId, reportId);
        
        // Build response
        User reporter = userRepository.findById(reporterUserId).orElse(null);
        String reporterUsername = reporter != null ? reporter.getUsername() : null;
        
        return UserReportResponse.builder()
            .reportId(reportId)
            .reporterUserId(reporterUserId)
            .reporterUsername(reporterUsername)
            .reportedUserId(reportedUserId)
            .reportedUsername(reportedUser.getUsername())
            .reportCategory(request.getReportCategory())
            .reportReason(request.getReportReason())
            .reportDescription(request.getReportDescription())
            .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
            .build();
    }
    
    @Override
    public List<UserReportResponse> getMyReports(Long userId) {
        String reporterKey = REPORT_REPORTER_KEY_PREFIX + userId;
        Set<String> reportIds = redisTemplate.opsForSet().members(reporterKey);
        
        if (reportIds == null || reportIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<UserReportResponse> reports = new ArrayList<>();
        for (String reportId : reportIds) {
            try {
                String reportKey = REPORT_KEY_PREFIX + reportId;
                Map<Object, Object> reportData = redisTemplate.opsForHash().entries(reportKey);
                if (!reportData.isEmpty()) {
                    reports.add(buildReportResponseFromMap(reportData));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch report {}: {}", reportId, e.getMessage());
            }
        }
        
        // Sort by created_at descending
        reports.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        return reports;
    }
    
    @Override
    public UserReportResponse getReportById(Long reportId, Long userId) {
        String reportKey = REPORT_KEY_PREFIX + reportId;
        Map<Object, Object> reportData = redisTemplate.opsForHash().entries(reportKey);
        
        if (reportData.isEmpty()) {
            throw new ResourceNotFoundException("Report not found");
        }
        
        // Verify user has access (must be the reporter)
        String reporterUserId = (String) reportData.get("reporter_user_id");
        if (!reporterUserId.equals(userId.toString())) {
            throw new BusinessException("You don't have access to this report");
        }
        
        return buildReportResponseFromMap(reportData);
    }
    
    private UserReportResponse buildReportResponseFromMap(Map<Object, Object> reportData) {
        Long reportId = Long.parseLong(reportData.get("report_id").toString());
        Long reporterUserId = Long.parseLong(reportData.get("reporter_user_id").toString());
        Long reportedUserId = Long.parseLong(reportData.get("reported_user_id").toString());
        
        // Fetch user details
        User reporter = userRepository.findById(reporterUserId).orElse(null);
        User reported = userRepository.findById(reportedUserId).orElse(null);
        
        String reporterUsername = reporter != null ? reporter.getUsername() : null;
        String reportedUsername = reported != null ? reported.getUsername() : null;
        
        OffsetDateTime createdAt = OffsetDateTime.parse(reportData.get("created_at").toString());
        
        return UserReportResponse.builder()
            .reportId(reportId)
            .reporterUserId(reporterUserId)
            .reporterUsername(reporterUsername)
            .reportedUserId(reportedUserId)
            .reportedUsername(reportedUsername)
            .reportCategory((String) reportData.get("report_category"))
            .reportReason((String) reportData.get("report_reason"))
            .reportDescription((String) reportData.get("report_description"))
            .createdAt(createdAt)
            .build();
    }
}





