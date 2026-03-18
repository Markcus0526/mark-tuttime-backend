package com.tuttime.admin.service;

import com.tuttime.admin.dto.BlockedDeviceResponse;
import com.tuttime.admin.dto.BlockedIpResponse;
import com.tuttime.admin.dto.FraudAnalyticsResponse;
import com.tuttime.admin.dto.FraudFlagResponse;
import com.tuttime.admin.dto.WarningCategory;
import com.tuttime.admin.dto.WarningSeverity;
import com.tuttime.admin.dto.WhitelistedDeviceResponse;
import com.tuttime.admin.dto.WhitelistedIpResponse;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Admin Anti-Fraud Service Interface
 */
public interface AdminAntiFraudService {
    Page<FraudFlagResponse> listFraudFlags(org.springframework.data.domain.Pageable pageable, 
                                           Long userId, 
                                           WarningCategory category, 
                                           WarningSeverity severity, 
                                           Boolean resolved);
    void blockDevice(String deviceId, String reason);
    void unblockDevice(String deviceId);
    void blockIpAddress(String ipAddress, String reason);
    void unblockIpAddress(String ipAddress);
    FraudAnalyticsResponse getFraudAnalytics();
    
    // Whitelist functionality
    void whitelistDevice(String deviceId, String reason);
    void removeDeviceFromWhitelist(String deviceId);
    void whitelistIpAddress(String ipAddress, String reason);
    void removeIpFromWhitelist(String ipAddress);
    
    // List functionality
    Page<BlockedDeviceResponse> listBlockedDevices(org.springframework.data.domain.Pageable pageable);
    Page<BlockedIpResponse> listBlockedIps(org.springframework.data.domain.Pageable pageable);
    Page<WhitelistedDeviceResponse> listWhitelistedDevices(org.springframework.data.domain.Pageable pageable);
    Page<WhitelistedIpResponse> listWhitelistedIps(org.springframework.data.domain.Pageable pageable);
}

