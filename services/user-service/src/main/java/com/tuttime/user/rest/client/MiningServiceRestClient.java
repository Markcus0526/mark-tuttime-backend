package com.tuttime.user.rest.client;

import com.tuttime.user.dto.BoostHistoryItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST Client for Mining Service
 *
 * <p>This client provides methods to interact with Mining Service via REST.
 * 
 * @deprecated Use {@link com.tuttime.user.grpc.client.MiningServiceGrpcClient} instead.
 * All operations should use gRPC for better performance and consistency.
 */
@Deprecated
@Slf4j
@Component
@RequiredArgsConstructor
public class MiningServiceRestClient {

    private final RestTemplate restTemplate;
    
    @Value("${services.mining-service.url:http://mining-service:28083}")
    private String miningServiceUrl;


    /**
     * Gets video boost history for a user.
     *
     * @param userId The user ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of BoostHistoryItem
     */
    public Page<BoostHistoryItem> getVideoBoostHistory(Long userId, int page, int size) {
        try {
            // Use admin endpoint to get video boosts for a user
            String url = miningServiceUrl + "/api/v1/admin/boosts/video?userId=" + userId + "&page=" + page + "&size=" + size;
            log.debug("REST: Calling Mining Service GetVideoBoostHistory for userId: {}, page: {}, size: {}", userId, page, size);
            
            HttpHeaders headers = new HttpHeaders();
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
                    Map<String, Object> pageData = (Map<String, Object>) data;
                    return parseBoostPage(pageData, page, size);
                }
            }
            
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        } catch (RestClientException e) {
            log.error("REST error getting video boost history for user: {}", userId, e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
    }

    private Page<BoostHistoryItem> parseBoostPage(Map<String, Object> pageData, int page, int size) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.getOrDefault("content", new ArrayList<>());
        long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
        
        List<BoostHistoryItem> boosts = new ArrayList<>();
        for (Map<String, Object> item : content) {
            boosts.add(parseBoostItem(item));
        }
        
        return new PageImpl<>(boosts, PageRequest.of(page, size), totalElements);
    }

    private BoostHistoryItem parseBoostItem(Map<String, Object> item) {
        String startedAtStr = (String) item.getOrDefault("startedAt", "");
        String expiresAtStr = (String) item.getOrDefault("expiresAt", "");
        
        OffsetDateTime startedAt = null;
        OffsetDateTime expiresAt = null;
        
        try {
            if (!startedAtStr.isEmpty()) {
                startedAt = OffsetDateTime.parse(startedAtStr).withOffsetSameInstant(ZoneOffset.UTC);
            }
            if (!expiresAtStr.isEmpty()) {
                expiresAt = OffsetDateTime.parse(expiresAtStr).withOffsetSameInstant(ZoneOffset.UTC);
            }
        } catch (Exception e) {
            log.warn("Failed to parse boost timestamps: startedAt={}, expiresAt={}", startedAtStr, expiresAtStr);
        }
        
        // Calculate duration in hours
        Integer duration = null;
        if (startedAt != null && expiresAt != null) {
            duration = (int) java.time.Duration.between(startedAt, expiresAt).toHours();
        }
        
        return BoostHistoryItem.builder()
                .boostId(extractLong(item, "boostId"))
                .boostType("VIDEO_BOOST")
                .multiplier(extractBigDecimal(item, "boostAmount"))
                .duration(duration)
                .activatedAt(startedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private Long extractLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
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
}

