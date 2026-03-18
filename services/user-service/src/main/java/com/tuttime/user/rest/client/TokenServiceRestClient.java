package com.tuttime.user.rest.client;

import com.tuttime.user.dto.TransactionHistoryItem;
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
 * REST Client for Token Service
 *
 * <p>This client provides methods to interact with Token Service via REST.
 * Used for operations that don't have gRPC endpoints yet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenServiceRestClient {

    private final RestTemplate restTemplate;
    
    @Value("${services.token-service.url:http://token-service:28082}")
    private String tokenServiceUrl;


    /**
     * Gets transaction history for a user.
     *
     * @param userId The user ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of TransactionHistoryItem
     */
    public Page<TransactionHistoryItem> getTransactionHistory(Long userId, int page, int size) {
        try {
            String url = tokenServiceUrl + "/api/v1/tokens/transactions?page=" + page + "&size=" + size;
            log.debug("REST: Calling Token Service GetTransactionHistory for userId: {}, page: {}, size: {}", userId, page, size);
            
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
                    Map<String, Object> pageData = (Map<String, Object>) data;
                    return parseTransactionPage(pageData, page, size);
                }
            }
            
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        } catch (RestClientException e) {
            log.error("REST error getting transaction history for user: {}", userId, e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
    }

    private Page<TransactionHistoryItem> parseTransactionPage(Map<String, Object> pageData, int page, int size) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.getOrDefault("content", new ArrayList<>());
        long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
        
        List<TransactionHistoryItem> transactions = new ArrayList<>();
        for (Map<String, Object> item : content) {
            transactions.add(parseTransactionItem(item));
        }
        
        return new PageImpl<>(transactions, PageRequest.of(page, size), totalElements);
    }

    private TransactionHistoryItem parseTransactionItem(Map<String, Object> item) {
        String createdAtStr = (String) item.getOrDefault("createdAt", "");
        OffsetDateTime createdAt = null;
        if (!createdAtStr.isEmpty()) {
            try {
                createdAt = OffsetDateTime.parse(createdAtStr).withOffsetSameInstant(ZoneOffset.UTC);
            } catch (Exception e) {
                log.warn("Failed to parse createdAt: {}", createdAtStr);
            }
        }
        
        return TransactionHistoryItem.builder()
                .transactionId(extractLong(item, "transactionId"))
                .type((String) item.getOrDefault("type", ""))
                .amount(extractBigDecimal(item, "amount"))
                .status((String) item.getOrDefault("status", ""))
                .createdAt(createdAt)
                .referenceType((String) item.getOrDefault("referenceType", null))
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

