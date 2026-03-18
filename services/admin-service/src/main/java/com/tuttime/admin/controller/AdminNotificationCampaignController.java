package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.kafka.CampaignEventProducer;
import com.tuttime.admin.rest.client.NotificationServiceRestClient;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Controller for Notification Campaign Management
 * Handles campaign CRUD operations and publishes events to Kafka
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notifications/campaigns")
@Tag(name = "Admin Notification Campaign Management", description = "Admin APIs for notification campaign management")
@RequiredArgsConstructor
public class AdminNotificationCampaignController {

    private final NotificationServiceRestClient notificationServiceRestClient;
    private final AdminAuditService auditService;
    private final CampaignEventProducer campaignEventProducer; // Optional - only if Kafka is enabled

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List Campaigns", description = "Get list of notification campaigns with optional status filter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listCampaigns(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Listing campaigns, status filter: {}", adminId, status);
        
        Map<String, Object> pageData = notificationServiceRestClient.listCampaigns(pageable, status);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_VIEWED,
                "CAMPAIGN", null, "Listed notification campaigns");
        
        return ResponseEntity.ok(ApiResponse.success("Campaigns retrieved successfully", pageData));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get Campaign", description = "Get campaign details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Getting campaign: {}", adminId, id);
        
        Map<String, Object> campaign = notificationServiceRestClient.getCampaign(id);
        
        return ResponseEntity.ok(ApiResponse.success("Campaign retrieved successfully", campaign));
    }

    // Note: POST /api/admin/notifications/campaigns is handled by AdminNotificationController.createCampaign()
    // to avoid duplicate endpoint mapping conflict

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update Campaign", description = "Update notification campaign")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Updating campaign: {}", adminId, id);
        
        Map<String, Object> campaignRequest = new HashMap<>();
        if (request.getCampaignName() != null) {
            campaignRequest.put("campaignName", request.getCampaignName());
        }
        if (request.getSourceTitle() != null) {
            campaignRequest.put("sourceTitle", request.getSourceTitle());
        }
        if (request.getSourceMessage() != null) {
            campaignRequest.put("sourceMessage", request.getSourceMessage());
        }
        if (request.getChannels() != null) {
            campaignRequest.put("channels", request.getChannels());
        }
        if (request.getPriority() != null) {
            campaignRequest.put("priority", request.getPriority());
        }
        
        Map<String, Object> campaign = notificationServiceRestClient.updateCampaign(id, campaignRequest);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_UPDATED,
                "CAMPAIGN", id,
                "Updated campaign: " + id);
        
        return ResponseEntity.ok(ApiResponse.success("Campaign updated successfully", campaign));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete Campaign", description = "Delete notification campaign")
    public ResponseEntity<ApiResponse<Void>> deleteCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Deleting campaign: {}", adminId, id);
        
        notificationServiceRestClient.deleteCampaign(id);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_DELETED,
                "CAMPAIGN", id,
                "Deleted campaign: " + id);
        
        return ResponseEntity.ok(ApiResponse.success("Campaign deleted successfully", null));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Cancel Campaign", description = "Cancel a running campaign")
    public ResponseEntity<ApiResponse<Void>> cancelCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Cancelling campaign: {}", adminId, id);
        
        notificationServiceRestClient.cancelCampaign(id);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_UPDATED,
                "CAMPAIGN", id,
                "Cancelled campaign: " + id);
        
        return ResponseEntity.ok(ApiResponse.success("Campaign cancelled successfully", null));
    }
}
