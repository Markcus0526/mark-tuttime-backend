package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Device Controller
 * 
 * <p>This controller provides admin endpoints for device management.
 * These endpoints are used by admin-service for internal service-to-service communication.
 * 
 * <p><b>Endpoints:</b>
 * <ul>
 *   <li>GET /api/v1/admin/devices/{clientDeviceId} - Get device by client device ID</li>
 *   <li>PUT /api/v1/admin/devices/{clientDeviceId}/status - Update device status</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/devices")
@RequiredArgsConstructor
@Tag(name = "Admin Device Management", description = "Admin-only device management APIs")
public class AdminDeviceController {

    private final UserService userService;

    /**
     * Admin endpoint: Get device information by client device ID
     * 
     * <p>This endpoint is used by admin-service to query device information
     * when blocking/unblocking devices. It's under /api/v1/admin/** which is
     * public for internal service-to-service communication.
     * 
     * @param clientDeviceId Client device ID (UUID from mobile app)
     * @return Device information including userId, device details, etc.
     */
    @GetMapping("/{clientDeviceId}")
    @Operation(summary = "Get device by client device ID (Admin)", 
            description = "Admin endpoint to get device information by client device ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeviceByClientDeviceId(
            @PathVariable String clientDeviceId) {
        log.info("Admin: Getting device by clientDeviceId: {}", clientDeviceId);
        
        var device = userService.getDeviceByClientDeviceId(clientDeviceId);
        if (device == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", device.getDeviceId());
        deviceInfo.put("userId", device.getUserId());
        deviceInfo.put("clientDeviceId", device.getClientDeviceId() != null ? device.getClientDeviceId() : "");
        deviceInfo.put("deviceFingerprint", device.getDeviceFingerprint());
        deviceInfo.put("deviceType", device.getDeviceType() != null ? device.getDeviceType().name() : "");
        deviceInfo.put("deviceModel", device.getDeviceModel() != null ? device.getDeviceModel() : "");
        deviceInfo.put("osVersion", device.getOsVersion() != null ? device.getOsVersion() : "");
        deviceInfo.put("ipAddress", device.getIpAddress() != null ? device.getIpAddress() : "");
        deviceInfo.put("lastSeen", device.getLastSeen() != null ? device.getLastSeen().toString() : "");
        deviceInfo.put("isActive", device.getIsActive());
        deviceInfo.put("isTrusted", device.getIsTrusted());
        
        return ResponseEntity.ok(ApiResponse.success(deviceInfo));
    }

    /**
     * Admin endpoint: Get devices by IP address
     * 
     * <p>This endpoint is used by admin-service to find all devices with a specific IP address.
     * Used when blocking an IP address to also block all associated devices.
     * 
     * @param ipAddress IP address to search for
     * @return List of devices with the specified IP address
     */
    @GetMapping("/by-ip/{ipAddress}")
    @Operation(summary = "Get devices by IP address (Admin)", 
            description = "Admin endpoint to get all devices with a specific IP address")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> getDevicesByIpAddress(
            @PathVariable String ipAddress) {
        log.info("Admin: Getting devices by IP address: {}", ipAddress);
        
        var devices = userService.getDevicesByIpAddress(ipAddress);
        
        java.util.List<Map<String, Object>> deviceList = devices.stream()
                .map(device -> {
                    Map<String, Object> deviceInfo = new HashMap<>();
                    deviceInfo.put("deviceId", device.getDeviceId());
                    deviceInfo.put("userId", device.getUserId());
                    deviceInfo.put("clientDeviceId", device.getClientDeviceId() != null ? device.getClientDeviceId() : "");
                    deviceInfo.put("deviceFingerprint", device.getDeviceFingerprint());
                    deviceInfo.put("deviceType", device.getDeviceType() != null ? device.getDeviceType().name() : "");
                    deviceInfo.put("ipAddress", device.getIpAddress() != null ? device.getIpAddress() : "");
                    deviceInfo.put("isActive", device.getIsActive());
                    deviceInfo.put("isTrusted", device.getIsTrusted());
                    return deviceInfo;
                })
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(deviceList));
    }

    /**
     * Admin endpoint: Get blocked devices (isActive = false) with pagination
     * 
     * <p>This endpoint is used by admin-service to get all blocked devices from the database.
     * 
     * @param pageable Pagination parameters
     * @return Page of blocked devices
     */
    @GetMapping("/blocked")
    @Operation(summary = "Get blocked devices (Admin)", 
            description = "Admin endpoint to get all blocked devices (isActive=false) with pagination")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBlockedDevices(
            @PageableDefault(size = 50) Pageable pageable) {
        log.info("Admin: Getting blocked devices - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<com.tuttime.user.entity.UserDevice> devicePage = userService.getBlockedDevices(pageable);
        
        // Convert to Map format for admin-service
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("content", devicePage.getContent().stream()
                .map(device -> {
                    Map<String, Object> deviceInfo = new HashMap<>();
                    deviceInfo.put("deviceId", device.getDeviceId());
                    deviceInfo.put("userId", device.getUserId());
                    deviceInfo.put("clientDeviceId", device.getClientDeviceId() != null ? device.getClientDeviceId() : "");
                    deviceInfo.put("deviceFingerprint", device.getDeviceFingerprint());
                    deviceInfo.put("deviceType", device.getDeviceType() != null ? device.getDeviceType().name() : "");
                    deviceInfo.put("deviceModel", device.getDeviceModel() != null ? device.getDeviceModel() : "");
                    deviceInfo.put("osVersion", device.getOsVersion() != null ? device.getOsVersion() : "");
                    deviceInfo.put("ipAddress", device.getIpAddress() != null ? device.getIpAddress() : "");
                    deviceInfo.put("lastSeen", device.getLastSeen() != null ? device.getLastSeen().toString() : "");
                    deviceInfo.put("isActive", device.getIsActive());
                    deviceInfo.put("isTrusted", device.getIsTrusted());
                    deviceInfo.put("createdAt", device.getCreatedAt() != null ? device.getCreatedAt().toString() : "");
                    deviceInfo.put("updatedAt", device.getUpdatedAt() != null ? device.getUpdatedAt().toString() : "");
                    return deviceInfo;
                })
                .collect(java.util.stream.Collectors.toList()));
        pageData.put("totalElements", devicePage.getTotalElements());
        pageData.put("totalPages", devicePage.getTotalPages());
        pageData.put("number", devicePage.getNumber());
        pageData.put("size", devicePage.getSize());
        pageData.put("first", devicePage.isFirst());
        pageData.put("last", devicePage.isLast());
        
        return ResponseEntity.ok(ApiResponse.success(pageData));
    }

    /**
     * Admin endpoint: Update device status (block/unblock device)
     * 
     * <p>This endpoint is used by admin-service to update device status in the database.
     * When blocking: sets isActive=false, isTrusted=false
     * When unblocking: sets isActive=true
     * 
     * @param clientDeviceId Client device ID (UUID from mobile app)
     * @param isActive Whether the device should be active (optional)
     * @param isTrusted Whether the device should be trusted (optional)
     * @return Updated device information
     */
    @PutMapping("/{clientDeviceId}/status")
    @Operation(summary = "Update device status (Admin)", 
            description = "Admin endpoint to update device status (isActive, isTrusted)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDeviceStatus(
            @PathVariable String clientDeviceId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isTrusted) {
        log.info("Admin: Updating device status - clientDeviceId: {}, isActive: {}, isTrusted: {}", 
                clientDeviceId, isActive, isTrusted);
        
        var device = userService.updateDeviceStatus(clientDeviceId, isActive, isTrusted);
        if (device == null) {
            log.warn("Admin: Device not found - clientDeviceId: {}", clientDeviceId);
            throw new ResourceNotFoundException("Device not found with clientDeviceId: " + clientDeviceId);
        }
        
        log.info("Admin: Device status updated successfully - deviceId: {}, isActive: {}, isTrusted: {}", 
                device.getDeviceId(), device.getIsActive(), device.getIsTrusted());
        
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", device.getDeviceId());
        deviceInfo.put("userId", device.getUserId());
        deviceInfo.put("clientDeviceId", device.getClientDeviceId() != null ? device.getClientDeviceId() : "");
        deviceInfo.put("isActive", device.getIsActive());
        deviceInfo.put("isTrusted", device.getIsTrusted());
        
        return ResponseEntity.ok(ApiResponse.success("Device status updated successfully", deviceInfo));
    }
}

