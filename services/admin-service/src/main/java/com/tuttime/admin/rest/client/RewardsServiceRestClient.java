package com.tuttime.admin.rest.client;

import com.tuttime.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * REST Client for Rewards Service
 * 
 * <p>Handles HTTP communication with rewards-service for referral data.
 */
@Slf4j
@Component
public class RewardsServiceRestClient {

    private final WebClient.Builder webClientBuilder;
    private final String rewardsServiceUrl;

    public RewardsServiceRestClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.rewards-service.url:http://localhost:28085}") String rewardsServiceUrl) {
        this.webClientBuilder = webClientBuilder;
        this.rewardsServiceUrl = rewardsServiceUrl;
    }

    private WebClient getWebClient() {
        return webClientBuilder.clone()
                .baseUrl(rewardsServiceUrl)
                .build();
    }

    /**
     * Get referral information for a user
     * 
     * @param userId User ID
     * @return Map containing referral info (referralCode, totalInvitedCount, activeMinersCount, etc.)
     */
    public Map<String, Object> getReferralInfo(Long userId) {
        try {
            log.debug("Getting referral info from rewards-service: userId={}", userId);
            
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/v1/referral/info")
                    .header("X-User-Id", String.valueOf(userId))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            
            log.warn("Referral info response missing data: userId={}", userId);
            return Map.of();
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // Connection refused or service unavailable - return empty map
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Rewards-service is not available (connection refused/timeout). Returning empty referral info. URL: {}", rewardsServiceUrl);
                return Map.of();
            }
            log.error("Network error getting referral info from rewards-service: userId={}", userId, e);
            return Map.of();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("User has no referral info yet: userId={}", userId);
            return Map.of();
        } catch (WebClientResponseException e) {
            log.error("Error getting referral info from rewards-service: userId={}, status={}, message={}", 
                    userId, e.getStatusCode(), e.getMessage());
            return Map.of();
        } catch (Exception e) {
            // Check if it's a connection error
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("Rewards-service is not available. Returning empty referral info. URL: {}", rewardsServiceUrl);
                return Map.of();
            }
            log.error("Unexpected error getting referral info: userId={}", userId, e);
            return Map.of();
        }
    }

    /**
     * Get friends status (referred users) for a user
     * 
     * @param userId User ID
     * @return List of maps containing friend status (fullName, profilePictureUrl, miningStatus)
     */
    public List<Map<String, Object>> getFriendsStatus(Long userId) {
        try {
            log.debug("Getting friends status from rewards-service: userId={}", userId);
            
            ApiResponse<List<Map<String, Object>>> response = getWebClient()
                    .get()
                    .uri("/api/v1/referral/friends")
                    .header("X-User-Id", String.valueOf(userId))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            
            log.warn("Friends status response missing data: userId={}", userId);
            return List.of();
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // Connection refused or service unavailable - return empty list
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Rewards-service is not available (connection refused/timeout). Returning empty friends status. URL: {}", rewardsServiceUrl);
                return List.of();
            }
            log.error("Network error getting friends status from rewards-service: userId={}", userId, e);
            return List.of();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("User has no friends: userId={}", userId);
            return List.of();
        } catch (WebClientResponseException e) {
            log.error("Error getting friends status from rewards-service: userId={}, status={}, message={}", 
                    userId, e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            // Check if it's a connection error
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("Rewards-service is not available. Returning empty friends status. URL: {}", rewardsServiceUrl);
                return List.of();
            }
            log.error("Unexpected error getting friends status: userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * Create referral relationship between two users
     * 
     * @param referrerId The user who is referring
     * @param refereeId The user being referred
     * @param level Optional referral level (1=direct, 2=indirect, etc.). If null, will be auto-calculated
     * @return Success message
     */
    public String createReferral(Long referrerId, Long refereeId, Integer level) {
        try {
            log.debug("Creating referral from rewards-service: referrerId={}, refereeId={}, level={}", referrerId, refereeId, level);
            
            StringBuilder uriBuilder = new StringBuilder("/api/v1/rewards/referrals?referrerId=")
                    .append(referrerId)
                    .append("&refereeId=")
                    .append(refereeId);
            if (level != null) {
                uriBuilder.append("&level=").append(level);
            }
            
            ApiResponse<String> response = getWebClient()
                    .post()
                    .uri(uriBuilder.toString())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<String>>() {})
                    .block();
            
            if (response != null && response.getSuccess() != null && response.getSuccess()) {
                log.info("Referral created successfully: referrerId={}, refereeId={}, level={}", referrerId, refereeId, level);
                return response.getData() != null ? response.getData() : response.getMessage();
            }
            
            String errorMsg = response != null ? response.getMessage() : "Unknown error";
            log.error("Failed to create referral: referrerId={}, refereeId={}, error={}", referrerId, refereeId, errorMsg);
            throw new RuntimeException("Failed to create referral: " + errorMsg);
        } catch (WebClientResponseException e) {
            log.error("Error creating referral from rewards-service: referrerId={}, refereeId={}, status={}, message={}", 
                    referrerId, refereeId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to create referral: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error creating referral: referrerId={}, refereeId={}", referrerId, refereeId, e);
            throw new RuntimeException("Failed to create referral: " + e.getMessage(), e);
        }
    }

    /**
     * Hard delete user (complete delete) from rewards-service admin endpoint
     */
    public void hardDeleteUser(Long userId) {
        try {
            log.debug("Calling rewards-service admin endpoint to hard delete user: userId={}", userId);
            getWebClient()
                    .delete()
                    .uri("/api/v1/admin/rewards/users/{userId}/hard-delete", userId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("User hard deleted from rewards-service successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling rewards-service to hard delete user: userId={}", userId, e);
            throw new RuntimeException("Failed to hard delete user from rewards-service: " + e.getMessage(), e);
        }
    }
}

