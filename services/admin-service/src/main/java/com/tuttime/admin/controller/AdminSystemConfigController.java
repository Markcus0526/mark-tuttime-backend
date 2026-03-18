package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminSystemConfigService;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Admin System Configuration Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@Tag(name = "Admin System Configuration", description = "System configuration management APIs")
public class AdminSystemConfigController {

    private final AdminSystemConfigService systemConfigService;
    private final AdminAuditService auditService;
    private final CacheManager cacheManager;

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get system configuration", description = "Get current system configuration")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getSystemConfig(
            @AuthenticationPrincipal Long adminId) {
        
        long startTime = System.currentTimeMillis();
        log.info("=== STEP 1: Controller method called - adminId: {} ===", adminId);
        
        try {
            log.info("=== STEP 2: Calling systemConfigService.getSystemConfig() ===");
            SystemConfigResponse config = systemConfigService.getSystemConfig();
            log.info("=== STEP 3: Service returned - config is {} ===", config != null ? "NOT NULL" : "NULL");
            
            if (config == null) {
                log.error("=== STEP 3 ERROR: System config response is null ===");
                return ResponseEntity.ok(ApiResponse.error("System configuration is null"));
            }
            
            // Log config details for debugging (simplified to avoid serialization issues)
            log.info("=== STEP 4: Config summary - baseMiningSpeed: {}, maxMiningSpeed: {}, stakingPools: {}, stakingTiers: {}, statusLevels: {} ===", 
                    config.getBaseMiningSpeed(), 
                    config.getMaxMiningSpeed(),
                    config.getStakingPools() != null ? config.getStakingPools().size() : 0,
                    config.getStakingTiers() != null ? config.getStakingTiers().size() : 0,
                    config.getStatusLevels() != null ? config.getStatusLevels().size() : 0);
            
            log.info("=== STEP 5: Creating ApiResponse wrapper ===");
            ApiResponse<SystemConfigResponse> apiResponse = ApiResponse.success("System configuration retrieved successfully", config);
            log.info("=== STEP 6: ApiResponse created - success: {}, data is {} ===", 
                    apiResponse.getSuccess(), apiResponse.getData() != null ? "NOT NULL" : "NULL");
            
            if (apiResponse == null || apiResponse.getData() == null) {
                log.error("=== STEP 6 ERROR: API response or data is null ===");
                return ResponseEntity.ok(ApiResponse.error("Failed to build system configuration response"));
            }
            
            log.info("=== STEP 7: Creating ResponseEntity ===");
            
            // Try to serialize the response to check for serialization issues
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.findAndRegisterModules(); // Register Java 8 time modules
                String jsonString = objectMapper.writeValueAsString(apiResponse);
                log.info("=== STEP 7a: JSON serialization successful - Response size: {} bytes ===", jsonString.length());
                if (jsonString.length() > 1000) {
                    log.info("=== STEP 7b: First 500 chars of JSON: {} ===", jsonString.substring(0, Math.min(500, jsonString.length())));
                    log.info("=== STEP 7c: Last 200 chars of JSON: {} ===", jsonString.substring(Math.max(0, jsonString.length() - 200)));
                } else {
                    log.info("=== STEP 7b: Full JSON: {} ===", jsonString);
                }
            } catch (Exception e) {
                log.error("=== STEP 7 ERROR: JSON serialization failed: {} ===", e.getMessage(), e);
                e.printStackTrace();
                // Continue anyway - let Spring handle it, but this indicates a problem
            }
            
            ResponseEntity<ApiResponse<SystemConfigResponse>> response = ResponseEntity
                    .ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(apiResponse);
            log.info("=== STEP 8: ResponseEntity created - Status: {}, Has body: {} ===", 
                    response.getStatusCode(), response.hasBody());
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("=== STEP 9: Returning response - Total time: {}ms ===", duration);
            
            return response;
        } catch (BusinessException e) {
            log.error("=== BUSINESS EXCEPTION in getSystemConfig: {} ===", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("=== UNEXPECTED ERROR in getSystemConfig: {} ===", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve system configuration: " + e.getMessage()));
        }
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update system configuration", description = "Update system configuration (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> updateSystemConfig(
            @Valid @RequestBody UpdateSystemConfigRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        SystemConfigResponse config = systemConfigService.updateSystemConfig(request, adminId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "SYSTEM", null, "Updated system configuration");
        
        return ResponseEntity.ok(ApiResponse.success("System configuration updated successfully", config));
    }

    @PostMapping("/cache/clear")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Clear all caches", description = "Clear all Redis caches (useful when cache format changes)")
    public ResponseEntity<ApiResponse<String>> clearAllCaches(
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Clearing all caches requested by admin: {}", adminId);
        
        try {
            // Clear all cache names
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.info("Cleared cache: {}", cacheName);
                }
            });
            
            auditService.logAction(adminId, null,
                    com.tuttime.admin.entity.AdminAuditLog.AdminActionType.BULK_OPERATION,
                    "SYSTEM", null, "Cleared all caches");
            
            return ResponseEntity.ok(ApiResponse.success("All caches cleared successfully"));
        } catch (Exception e) {
            log.error("Error clearing caches", e);
            throw new BusinessException("Failed to clear caches: " + e.getMessage());
        }
    }

    @PostMapping("/cache/clear/{cacheName}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Clear specific cache", description = "Clear a specific cache by name (e.g., 'analytics', 'userDetails')")
    public ResponseEntity<ApiResponse<String>> clearCache(
            @PathVariable String cacheName,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Clearing cache '{}' requested by admin: {}", cacheName, adminId);
        
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("Cleared cache: {}", cacheName);
                
                auditService.logAction(adminId, null,
                        com.tuttime.admin.entity.AdminAuditLog.AdminActionType.BULK_OPERATION,
                        "SYSTEM", null, "Cleared cache: " + cacheName);
                
                return ResponseEntity.ok(ApiResponse.success("Cache '" + cacheName + "' cleared successfully"));
            } else {
                return ResponseEntity.ok(ApiResponse.success("Cache '" + cacheName + "' not found (may not exist)"));
            }
        } catch (Exception e) {
            log.error("Error clearing cache: {}", cacheName, e);
            throw new BusinessException("Failed to clear cache: " + e.getMessage());
        }
    }

    @GetMapping("/config/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Test system config endpoint", description = "Simple test endpoint to verify the API is working")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testSystemConfig(
            @AuthenticationPrincipal Long adminId) {
        
        log.info("=== TEST ENDPOINT: Called by adminId: {} ===", adminId);
        Map<String, Object> testData = Map.of(
                "message", "System config endpoint is working",
                "timestamp", java.time.OffsetDateTime.now().toString(),
                "adminId", adminId != null ? adminId.toString() : "null"
        );
        log.info("=== TEST ENDPOINT: Returning response ===");
        return ResponseEntity.ok(ApiResponse.success("Test successful", testData));
    }

    @GetMapping("/config/minimal")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Minimal system config", description = "Returns minimal system config without external service calls")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getMinimalSystemConfig(
            @AuthenticationPrincipal Long adminId) {
        
        log.info("=== MINIMAL ENDPOINT: Called by adminId: {} ===", adminId);
        
        try {
            // Build minimal response without calling external services
            SystemConfigResponse minimalConfig = SystemConfigResponse.builder()
                    .baseMiningSpeed(new java.math.BigDecimal("0.3"))
                    .maxMiningSpeed(new java.math.BigDecimal("77.777"))
                    .minMiningIntervalMinutes(1)
                    .maxMiningIntervalMinutes(5)
                    .monthlyPoolAmount(new java.math.BigDecimal("50000000000"))
                    .maxReferralBoosts(10)
                    .bookingBoostBase(new java.math.BigDecimal("2"))
                    .bookingBoostDurationDays(15)
                    .rentalBoostMultiplier(new java.math.BigDecimal("2"))
                    .rentalBoostDurationDays(2)
                    .withdrawalPlatformFeePercent(java.math.BigDecimal.ZERO)
                    .stakingPools(List.of())
                    .stakingTiers(List.of())
                    .statusLevels(List.of())
                    .build();
            
            log.info("=== MINIMAL ENDPOINT: Returning minimal config ===");
            return ResponseEntity.ok(ApiResponse.success("Minimal system configuration", minimalConfig));
        } catch (Exception e) {
            log.error("=== MINIMAL ENDPOINT ERROR: {} ===", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Failed to build minimal config: " + e.getMessage()));
        }
    }
}

