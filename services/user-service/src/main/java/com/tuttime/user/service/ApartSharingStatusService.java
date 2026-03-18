package com.tuttime.user.service;

import com.tuttime.user.entity.User;
import com.tuttime.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/** Apart-sharing Status Service: caches registration status in database to avoid calling GraphQL API on every request. Checks cache first, calls API if missing/stale. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApartSharingStatusService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${apart-sharing.graphql.url:https://devteamapi.apart-sharing.com/graphql}")
    private String apartSharingGraphqlUrl;

    @Value("${apart-sharing.status.cache-ttl-hours:24}")
    private Integer cacheTtlHours;

    /** Gets Apart-sharing registration status: checks cached status, calls GraphQL API if missing/stale, updates database, returns status. */
    @Transactional
    public Map<String, Object> getApartSharingStatus(Long userId) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Check if we have cached status and if it's still valid
        boolean shouldRefresh = shouldRefreshCache(user);
        
        // Always refresh if:
        // 1. Cache is missing or stale
        // 2. Cache exists but registration status is false (user might have registered since last check)
        boolean shouldCallApi = shouldRefresh || 
                                (user.getApartSharingRegistered() != null && 
                                 Boolean.FALSE.equals(user.getApartSharingRegistered()));
        
        if (!shouldCallApi && user.getApartSharingRegistered() != null) {
            // Return cached status (only if registered = true and cache is fresh)
            log.debug("Returning cached Apart-sharing status for user: {} (registered: {})", 
                    userId, user.getApartSharingRegistered());
            return buildStatusMap(user);
        }

        // Cache is missing, stale, or user is not registered - fetch fresh status
        log.info("Fetching fresh Apart-sharing status for user: {} (phone: {}). Reason: {}", 
                userId, user.getPhone(), 
                shouldRefresh ? "cache missing/stale" : "user not registered, checking again");
        
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            log.warn("User {} has no phone number, cannot check Apart-sharing status", userId);
            return Map.of("isRegistered", false, "message", "Phone number not found");
        }

        try {
            Map<String, Object> status = fetchStatusFromGraphQL(user.getPhone());
            
            // Update user with fresh status
            updateUserStatus(user, status);
            userRepository.save(user);
            
            log.info("Updated Apart-sharing status for user {}: registered={}, verified={}", 
                    userId, user.getApartSharingRegistered(), user.getApartSharingVerified());
            
            return buildStatusMap(user);
        } catch (Exception e) {
            log.error("Error fetching Apart-sharing status for user {}: {}", userId, e.getMessage(), e);
            
            // If we have cached status, return it even if stale
            if (user.getApartSharingRegistered() != null) {
                log.warn("Returning stale cached status due to API error");
                return buildStatusMap(user);
            }
            
            // No cache available, return error
            return Map.of("error", "Failed to fetch registration status: " + e.getMessage());
        }
    }

    /**
     * Checks if cache should be refreshed.
     * 
     * @param user The user entity
     * @return true if cache should be refreshed
     */
    private boolean shouldRefreshCache(User user) {
        if (user.getApartSharingStatusCheckedAt() == null) {
            return true; // No cache exists
        }
        
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime cacheExpiry = user.getApartSharingStatusCheckedAt()
                .plusHours(cacheTtlHours);
        
        return now.isAfter(cacheExpiry);
    }

    /**
     * Fetches status from Apart-sharing GraphQL API.
     */
    private Map<String, Object> fetchStatusFromGraphQL(String phone) {
        String graphqlQuery = String.format(
            "{\"query\": \"query { user_exists(phone: \\\"%s\\\") { id fio rating is_exists is_verified is_blocked is_russian verified_status_text { title color } is_phone_confirmed status status_message } }\"}",
            phone
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(graphqlQuery, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            apartSharingGraphqlUrl,
            HttpMethod.POST,
            entity,
            String.class
        );
        
        // Parse GraphQL response
        JsonNode jsonResponse;
        try {
            jsonResponse = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse GraphQL response: " + e.getMessage(), e);
        }
        
        // Check for errors
        if (jsonResponse.has("errors")) {
            JsonNode errors = jsonResponse.get("errors");
            String errorMessage = errors.isArray() && errors.size() > 0 
                    ? errors.get(0).get("message").asText() 
                    : "Unknown GraphQL error";
            throw new RuntimeException("GraphQL API error: " + errorMessage);
        }
        
        JsonNode data = jsonResponse.get("data");
        JsonNode userExists = data != null ? data.get("user_exists") : null;
        
        if (userExists == null || !userExists.get("is_exists").asBoolean()) {
            return Map.of(
                "isRegistered", false,
                "message", "User not found in Apart-sharing"
            );
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("isRegistered", true);
        status.put("isVerified", userExists.get("is_verified").asBoolean());
        status.put("isBlocked", userExists.get("is_blocked").asBoolean());
        status.put("isPhoneConfirmed", userExists.get("is_phone_confirmed").asBoolean());
        status.put("status", userExists.get("status").asText());
        status.put("statusMessage", userExists.has("status_message") && !userExists.get("status_message").isNull() 
                ? userExists.get("status_message").asText() : null);
        
        if (userExists.has("verified_status_text") && !userExists.get("verified_status_text").isNull()) {
            JsonNode verifiedStatus = userExists.get("verified_status_text");
            status.put("verifiedStatusTitle", verifiedStatus.get("title").asText());
            status.put("verifiedStatusColor", verifiedStatus.get("color").asText());
        }
        
        // Store full response as JSON
        status.put("_fullResponse", jsonResponse.toString());
        
        return status;
    }

    /**
     * Updates user entity with status information.
     */
    private void updateUserStatus(User user, Map<String, Object> status) {
        if (status.containsKey("error")) {
            // Don't update on error
            return;
        }
        
        user.setApartSharingRegistered((Boolean) status.getOrDefault("isRegistered", false));
        user.setApartSharingVerified((Boolean) status.getOrDefault("isVerified", false));
        user.setApartSharingStatus((String) status.get("status"));
        user.setApartSharingStatusCheckedAt(OffsetDateTime.now(ZoneOffset.UTC));
        
        // Store full response data
        if (status.containsKey("_fullResponse")) {
            user.setApartSharingStatusData((String) status.get("_fullResponse"));
        }
    }

    /**
     * Builds status map from user entity.
     */
    private Map<String, Object> buildStatusMap(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("isRegistered", user.getApartSharingRegistered() != null && user.getApartSharingRegistered());
        result.put("isVerified", user.getApartSharingVerified() != null && user.getApartSharingVerified());
        result.put("status", user.getApartSharingStatus());
        result.put("statusCheckedAt", user.getApartSharingStatusCheckedAt());
        
        // Parse full response data if available
        if (user.getApartSharingStatusData() != null) {
            try {
                JsonNode fullData = objectMapper.readTree(user.getApartSharingStatusData());
                JsonNode userExists = fullData.get("data").get("user_exists");
                if (userExists != null) {
                    result.put("isBlocked", userExists.get("is_blocked").asBoolean());
                    result.put("isPhoneConfirmed", userExists.get("is_phone_confirmed").asBoolean());
                    result.put("statusMessage", userExists.has("status_message") && !userExists.get("status_message").isNull()
                            ? userExists.get("status_message").asText() : null);
                    if (userExists.has("verified_status_text") && !userExists.get("verified_status_text").isNull()) {
                        JsonNode verifiedStatus = userExists.get("verified_status_text");
                        result.put("verifiedStatusTitle", verifiedStatus.get("title").asText());
                        result.put("verifiedStatusColor", verifiedStatus.get("color").asText());
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse stored status data for user {}: {}", user.getUserId(), e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * Forces a refresh of Apart-sharing status for a user.
     * 
     * @param userId The user ID
     * @return Updated status map
     */
    @Transactional
    public Map<String, Object> refreshApartSharingStatus(Long userId) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Clear cache timestamp to force refresh
        user.setApartSharingStatusCheckedAt(null);
        userRepository.save(user);
        
        // Fetch fresh status
        return getApartSharingStatus(userId);
    }
}

