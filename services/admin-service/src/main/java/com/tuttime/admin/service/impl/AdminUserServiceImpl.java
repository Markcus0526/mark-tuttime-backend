package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.BlockedUserResponse;
import com.tuttime.admin.rest.client.UserServiceRestClient;
import com.tuttime.admin.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Admin User Service Implementation
 * 
 * <p>Provides user management functionality for admin panel.
 * Uses gRPC to communicate with user-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserServiceRestClient userServiceRestClient;

    @Override
    public Page<BlockedUserResponse> listBlockedUsers(Pageable pageable) {
        log.debug("Listing blocked users via user-service");
        
        // Call user-service admin endpoint - returns Map
        Map<String, Object> response = userServiceRestClient.getBlockedUsers(pageable);
        
        // Extract page data from Map
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getOrDefault("content", List.of());
        long totalElements = ((Number) response.getOrDefault("totalElements", 0L)).longValue();
        int totalPages = ((Number) response.getOrDefault("totalPages", 0)).intValue();
        int number = ((Number) response.getOrDefault("number", 0)).intValue();
        int size = ((Number) response.getOrDefault("size", 20)).intValue();
        
        // Map to admin DTO
        List<BlockedUserResponse> blockedUsers = content.stream()
                .map(userMap -> BlockedUserResponse.builder()
                        .userId(userMap.get("userId") != null ? 
                                ((Number) userMap.get("userId")).longValue() : null)
                        .email((String) userMap.get("email"))
                        .phone((String) userMap.get("phone"))
                        .blockedReason((String) userMap.get("blockedReason"))
                        .blockedAt(parseDateTime(userMap.get("blockedAt")))
                        .blockedBy(null) // TODO: Add blockedBy field to user-service entity
                        .build())
                .collect(java.util.stream.Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(
                blockedUsers, 
                org.springframework.data.domain.PageRequest.of(number, size), 
                totalElements);
    }
    
    /**
     * Helper method to parse date/time from Map value.
     * 
     * <p><b>Timezone Note:</b> Parsed dates are assumed to be in UTC timezone.
     * All API responses serialize dates in UTC, so this is safe.
     * 
     * @param dateValue The datetime value to parse (String or null)
     * @return Parsed OffsetDateTime in UTC, or null if parsing fails
     */
    private OffsetDateTime parseDateTime(Object dateValue) {
        if (dateValue == null) {
            return null;
        }
        if (dateValue instanceof String) {
            try {
                // Parse as OffsetDateTime
                return OffsetDateTime.parse((String) dateValue).withOffsetSameInstant(ZoneOffset.UTC);
            } catch (Exception e) {
                log.warn("Failed to parse date: {}", dateValue, e);
                return null;
            }
        }
        return null;
    }
}

