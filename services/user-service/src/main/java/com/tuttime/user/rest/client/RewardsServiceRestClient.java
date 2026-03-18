package com.tuttime.user.rest.client;

import com.tuttime.user.dto.ReferralInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST Client for Rewards Service
 *
 * <p>This client provides methods to interact with Rewards Service via REST.
 * Used for operations that don't have gRPC endpoints yet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RewardsServiceRestClient {

    private final RestTemplate restTemplate;
    
    @Value("${services.rewards-service.url:http://rewards-service:28085}")
    private String rewardsServiceUrl;
    
    /**
     * Gets the rewards service URL (for use by other services).
     */
    public String getRewardsServiceUrl() {
        return rewardsServiceUrl;
    }

    /**
     * Gets referral information for a user.
     *
     * @param userId The user ID
     * @return ReferralInfoResponse containing referral statistics
     */
    public ReferralInfoResponse getReferralInfo(Long userId) {
        try {
            String url = rewardsServiceUrl + "/api/v1/referral/info";
            log.debug("REST: Calling Rewards Service GetReferralInfo for userId: {}", userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId.toString());
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ParameterizedTypeReference<Map<String, Object>> responseType = 
                    new ParameterizedTypeReference<Map<String, Object>>() {};
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType
            );
            
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object data = body.get("data");
                
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    return ReferralInfoResponse.builder()
                            .referralCode((String) dataMap.getOrDefault("referralCode", ""))
                            .totalReferrals(extractInt(dataMap, "totalInvitedCount"))
                            .activeReferrals(extractInt(dataMap, "activeMinersCount"))
                            .totalEarned(extractBigDecimal(dataMap, "totalEarned"))
                            .build();
                }
            }
            
            return ReferralInfoResponse.empty();
        } catch (RestClientException e) {
            log.error("REST error getting referral info for user: {}", userId, e);
            return ReferralInfoResponse.empty();
        }
    }

    private int extractInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private BigDecimal extractBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value != null) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse BigDecimal for key {}: {}", key, value);
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Creates a referral relationship between two users.
     *
     * @param referrerId The ID of the user who made the referral
     * @param refereeId The ID of the new user who was referred
     * @param level Optional referral level (will be auto-calculated if null)
     */
    public void createReferral(Long referrerId, Long refereeId, Integer level) {
        try {
            String url = rewardsServiceUrl + "/api/v1/rewards/referrals";
            log.debug("REST: Calling Rewards Service CreateReferral for referrerId: {}, refereeId: {}, level: {}", 
                    referrerId, refereeId, level);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            // No X-User-Id needed as this is an internal call from user-service to rewards-service
            
            // Build query parameters
            String params = String.format("?referrerId=%d&refereeId=%d", referrerId, refereeId);
            if (level != null) {
                params += "&level=" + level;
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url + params, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Referral created successfully via REST: referrerId={}, refereeId={}, level={}", 
                        referrerId, refereeId, level);
            } else {
                log.error("Failed to create referral via REST: referrerId={}, refereeId={}, level={}, status={}, response={}", 
                        referrerId, refereeId, level, response.getStatusCode(), response.getBody());
            }
        } catch (RestClientException e) {
            log.error("REST error creating referral for referrerId: {}, refereeId: {}, level: {}", 
                    referrerId, refereeId, level, e);
            // Log and suppress, as referral creation shouldn't block user signup
        }
    }
}

