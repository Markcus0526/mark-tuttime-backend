package com.tuttime.admin.rest.client;

import com.tuttime.admin.dto.BookingBoostResponse;
import com.tuttime.admin.dto.RentalBoostResponse;
import com.tuttime.admin.dto.VideoBoostResponse;
import com.tuttime.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Client for Mining Service
 * Used for admin operations to query boost data
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiningServiceRestClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.mining-service.url:http://localhost:28083}")
    private String miningServiceUrl;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("MiningServiceRestClient initialized with miningServiceUrl: {}", miningServiceUrl);
        
        // Validate that the URL points to mining-service, not user-service or other services
        if (miningServiceUrl == null) {
            log.error("CRITICAL CONFIGURATION ERROR: miningServiceUrl is null!");
            throw new IllegalStateException("miningServiceUrl cannot be null");
        }
        
        String lowerUrl = miningServiceUrl.toLowerCase();
        if (lowerUrl.contains("user-service") || lowerUrl.contains("user_service")) {
            log.error("CRITICAL CONFIGURATION ERROR: miningServiceUrl is incorrectly set to user-service! URL: {}. This will cause 403 Forbidden errors.", miningServiceUrl);
            throw new IllegalStateException("miningServiceUrl is incorrectly configured. It points to user-service instead of mining-service. URL: " + miningServiceUrl);
        }
        
        if (lowerUrl.contains("token-service")) {
            log.error("CRITICAL CONFIGURATION ERROR: miningServiceUrl is set to token-service instead of mining-service! URL: {}", miningServiceUrl);
        }
        
        // Validate port (mining-service should be on 28083)
        if (miningServiceUrl.contains(":28081")) {
            log.error("CRITICAL CONFIGURATION ERROR: miningServiceUrl uses port 28081 (user-service port) instead of 28083 (mining-service port)! URL: {}", miningServiceUrl);
            throw new IllegalStateException("miningServiceUrl uses wrong port (28081 is user-service, should be 28083 for mining-service). URL: " + miningServiceUrl);
        }
        
        log.info("MiningServiceRestClient configuration validated successfully. URL: {}", miningServiceUrl);
    }

    private WebClient getWebClient() {
        return webClientBuilder.clone()
                .baseUrl(miningServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Page<BookingBoostResponse> listBookingBoosts(Pageable pageable, Long userId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> {
                        org.springframework.web.util.UriBuilder builder = uriBuilder
                                .path("/api/admin/boosts/bookings")
                                .queryParam("page", pageable.getPageNumber())
                                .queryParam("size", pageable.getPageSize());
                        if (userId != null) {
                            builder.queryParam("userId", userId);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && response.getData() != null) {
                Map<String, Object> pageData = response.getData();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentMaps = (List<Map<String, Object>>) pageData.getOrDefault("content", new java.util.ArrayList<>());
                List<BookingBoostResponse> content = contentMaps.stream()
                        .map(this::mapToBookingBoostResponse)
                        .collect(Collectors.toList());

                long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
                int number = ((Number) pageData.getOrDefault("number", pageable.getPageNumber())).intValue();
                int size = ((Number) pageData.getOrDefault("size", pageable.getPageSize())).intValue();

                return new PageImpl<>(content, PageRequest.of(number, size, pageable.getSort()), totalElements);
            }
            return new PageImpl<>(List.of());
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // Connection refused or service unavailable - return empty page
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Mining-service is not available (connection refused/timeout). Returning empty booking boosts list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Network error listing booking boosts: userId={}", userId, e);
            throw new RuntimeException("Failed to list booking boosts: " + e.getMessage(), e);
        } catch (WebClientResponseException e) {
            log.error("Error listing booking boosts: userId={}", userId, e);
            throw new RuntimeException("Failed to list booking boosts: " + e.getMessage(), e);
        } catch (Exception e) {
            // Check if it's a connection error
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("Mining-service is not available. Returning empty booking boosts list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Unexpected error listing booking boosts: userId={}", userId, e);
            throw new RuntimeException("Failed to list booking boosts: " + e.getMessage(), e);
        }
    }

    public BookingBoostResponse getBookingBoost(Long id) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/admin/boosts/bookings/{id}", id)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToBookingBoostResponse(response.getData());
            }
            throw new RuntimeException("Booking boost not found: " + id);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Booking boost not found: " + id, e);
        } catch (WebClientResponseException e) {
            log.error("Error getting booking boost: {}", id, e);
            throw new RuntimeException("Failed to get booking boost: " + e.getMessage(), e);
        }
    }

    public Page<RentalBoostResponse> listRentalBoosts(Pageable pageable, Long userId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> {
                        org.springframework.web.util.UriBuilder builder = uriBuilder
                                .path("/api/admin/boosts/rentals")
                                .queryParam("page", pageable.getPageNumber())
                                .queryParam("size", pageable.getPageSize());
                        if (userId != null) {
                            builder.queryParam("userId", userId);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && response.getData() != null) {
                Map<String, Object> pageData = response.getData();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentMaps = (List<Map<String, Object>>) pageData.getOrDefault("content", new java.util.ArrayList<>());
                List<RentalBoostResponse> content = contentMaps.stream()
                        .map(this::mapToRentalBoostResponse)
                        .collect(Collectors.toList());

                long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
                int number = ((Number) pageData.getOrDefault("number", pageable.getPageNumber())).intValue();
                int size = ((Number) pageData.getOrDefault("size", pageable.getPageSize())).intValue();

                return new PageImpl<>(content, PageRequest.of(number, size, pageable.getSort()), totalElements);
            }
            return new PageImpl<>(List.of());
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // Connection refused or service unavailable - return empty page
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Mining-service is not available (connection refused/timeout). Returning empty rental boosts list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Network error listing rental boosts: userId={}", userId, e);
            throw new RuntimeException("Failed to list rental boosts: " + e.getMessage(), e);
        } catch (WebClientResponseException e) {
            log.error("Error listing rental boosts: userId={}", userId, e);
            throw new RuntimeException("Failed to list rental boosts: " + e.getMessage(), e);
        } catch (Exception e) {
            // Check if it's a connection error
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("Mining-service is not available. Returning empty rental boosts list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Unexpected error listing rental boosts: userId={}", userId, e);
            throw new RuntimeException("Failed to list rental boosts: " + e.getMessage(), e);
        }
    }

    public RentalBoostResponse getRentalBoost(Long id) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/admin/boosts/rentals/{id}", id)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToRentalBoostResponse(response.getData());
            }
            throw new RuntimeException("Rental boost not found: " + id);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Rental boost not found: " + id, e);
        } catch (WebClientResponseException e) {
            log.error("Error getting rental boost: {}", id, e);
            throw new RuntimeException("Failed to get rental boost: " + e.getMessage(), e);
        }
    }

    public Page<VideoBoostResponse> listVideoBoosts(Pageable pageable, Long userId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> {
                        org.springframework.web.util.UriBuilder builder = uriBuilder
                                .path("/api/admin/boosts/videos")
                                .queryParam("page", pageable.getPageNumber())
                                .queryParam("size", pageable.getPageSize());
                        if (userId != null) {
                            builder.queryParam("userId", userId);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && response.getData() != null) {
                Map<String, Object> pageData = response.getData();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentMaps = (List<Map<String, Object>>) pageData.getOrDefault("content", new java.util.ArrayList<>());
                List<VideoBoostResponse> content = contentMaps.stream()
                        .map(this::mapToVideoBoostResponse)
                        .collect(Collectors.toList());

                long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
                int number = ((Number) pageData.getOrDefault("number", pageable.getPageNumber())).intValue();
                int size = ((Number) pageData.getOrDefault("size", pageable.getPageSize())).intValue();

                return new PageImpl<>(content, PageRequest.of(number, size), totalElements);
            }
            return new PageImpl<>(List.of());
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // Connection refused or service unavailable - return empty page
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Mining-service is not available (connection refused/timeout). Returning empty video boosts list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Network error listing video boosts", e);
            throw new RuntimeException("Failed to list video boosts: " + e.getMessage(), e);
        } catch (WebClientResponseException e) {
            log.error("Error listing video boosts: status={}, message={}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to list video boosts: " + e.getMessage(), e);
        } catch (Exception e) {
            // Check if it's a connection error
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("Mining-service is not available. Returning empty video boosts list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Unexpected error listing video boosts", e);
            throw new RuntimeException("Failed to list video boosts: " + e.getMessage(), e);
        }
    }

    public VideoBoostResponse getVideoBoost(Long id) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/admin/boosts/videos/{id}", id)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToVideoBoostResponse(response.getData());
            }
            throw new RuntimeException("Video boost not found: " + id);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Video boost not found: " + id, e);
        } catch (WebClientResponseException e) {
            log.error("Error getting video boost: {}", id, e);
            throw new RuntimeException("Failed to get video boost: " + e.getMessage(), e);
        }
    }

    public List<Object> getBoostsByUserId(Long userId) {
        try {
            ApiResponse<List<Map<String, Object>>> response = getWebClient()
                    .get()
                    .uri("/api/admin/boosts/users/{userId}", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && response.getData() != null) {
                // Convert to a mix of BookingBoostResponse, RentalBoostResponse, and VideoBoostResponse
                // Check which type of boost based on which ID field is present
                return response.getData().stream()
                        .map(data -> {
                            Long rentalId = getLongValue(data, "rentalId");
                            Long bookingId = getLongValue(data, "bookingId");
                            Long videoId = getLongValue(data, "videoId");
                            
                            if (videoId != null && bookingId == null && rentalId == null) {
                                // This is a video boost
                                return mapToVideoBoostResponse(data);
                            } else if (rentalId != null && bookingId == null && videoId == null) {
                                // This is a rental boost
                                return mapToRentalBoostResponse(data);
                            } else {
                                // This is a booking boost (bookingId is set)
                                return mapToBookingBoostResponse(data);
                            }
                        })
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (WebClientResponseException e) {
            log.error("Error getting boosts for user: {}", userId, e);
            throw new RuntimeException("Failed to get boosts for user: " + e.getMessage(), e);
        }
    }

    private BookingBoostResponse mapToBookingBoostResponse(Map<String, Object> data) {
        OffsetDateTime expiresAt = parseDateTime(data.get("expiresAt"));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        
        Boolean isExpired = expiresAt != null && expiresAt.isBefore(now);
        Long daysRemaining = null;
        if (expiresAt != null && !isExpired) {
            daysRemaining = java.time.Duration.between(now, expiresAt).toDays();
        }

        return BookingBoostResponse.builder()
                .boostId(getLongValue(data, "boostId"))
                .userId(getLongValue(data, "userId"))
                .bookingId(getLongValue(data, "bookingId"))
                .boostAmount(getBigDecimalValue(data, "boostAmount"))
                .startedAt(parseDateTime(data.get("startedAt")))
                .expiresAt(expiresAt)
                .isActive(getBooleanValue(data, "isActive"))
                .isExpired(isExpired)
                .daysRemaining(daysRemaining)
                .createdAt(parseDateTime(data.get("createdAt")))
                .updatedAt(parseDateTime(data.get("updatedAt")))
                .build();
    }

    private RentalBoostResponse mapToRentalBoostResponse(Map<String, Object> data) {
        OffsetDateTime expiresAt = parseDateTime(data.get("expiresAt"));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        
        Boolean isExpired = expiresAt != null && expiresAt.isBefore(now);
        Long daysRemaining = null;
        if (expiresAt != null && !isExpired) {
            daysRemaining = java.time.Duration.between(now, expiresAt).toDays();
        }

        return RentalBoostResponse.builder()
                .boostId(getLongValue(data, "boostId"))
                .userId(getLongValue(data, "userId"))
                .rentalId(getLongValue(data, "rentalId")) // Rental boosts have rentalId (for apartment owners)
                .boostAmount(getBigDecimalValue(data, "boostAmount"))
                .startedAt(parseDateTime(data.get("startedAt")))
                .expiresAt(expiresAt)
                .isActive(getBooleanValue(data, "isActive"))
                .isExpired(isExpired)
                .daysRemaining(daysRemaining)
                .createdAt(parseDateTime(data.get("createdAt")))
                .updatedAt(parseDateTime(data.get("updatedAt")))
                .build();
    }

    private com.tuttime.admin.dto.VideoBoostResponse mapToVideoBoostResponse(Map<String, Object> data) {
        OffsetDateTime expiresAt = parseDateTime(data.get("expiresAt"));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        
        Boolean isExpired = expiresAt != null && expiresAt.isBefore(now);
        Long daysRemaining = null;
        if (expiresAt != null && !isExpired) {
            daysRemaining = java.time.Duration.between(now, expiresAt).toDays();
        }

        return VideoBoostResponse.builder()
                .boostId(getLongValue(data, "boostId"))
                .userId(getLongValue(data, "userId"))
                .videoId(getLongValue(data, "videoId")) // Video boosts have videoId
                .boostAmount(getBigDecimalValue(data, "boostAmount"))
                .startedAt(parseDateTime(data.get("startedAt")))
                .expiresAt(expiresAt)
                .isActive(getBooleanValue(data, "isActive"))
                .isExpired(isExpired)
                .daysRemaining(daysRemaining)
                .createdAt(parseDateTime(data.get("createdAt")))
                .updatedAt(parseDateTime(data.get("updatedAt")))
                .build();
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return false;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private java.math.BigDecimal getBigDecimalValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return java.math.BigDecimal.ZERO;
        if (value instanceof java.math.BigDecimal) {
            return (java.math.BigDecimal) value;
        }
        if (value instanceof Number) {
            return java.math.BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new java.math.BigDecimal(value.toString());
    }

    /**
     * Parses a datetime string to OffsetDateTime.
     * 
     * <p><b>Timezone Note:</b> Parsed dates are assumed to be in UTC timezone.
     * All API responses serialize dates in UTC, so this is safe.
     * 
     * @param value The datetime value to parse (String or null)
     * @return Parsed OffsetDateTime in UTC context, or null if parsing fails
     */
    private OffsetDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String) {
                // Parses ISO-8601 format with timezone (assumed UTC from API responses)
                return OffsetDateTime.parse((String) value);
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", value, e);
            return null;
        }
    }

    /**
     * Lists all AP balances with pagination
     */
    public Page<Map<String, Object>> listApBalances(Pageable pageable) {
        try {
            // Runtime validation to ensure we're calling the correct service
            if (miningServiceUrl == null) {
                log.error("CRITICAL: miningServiceUrl is null!");
                throw new IllegalStateException("miningServiceUrl cannot be null");
            }
            String lowerUrl = miningServiceUrl.toLowerCase();
            if (lowerUrl.contains("user-service") || lowerUrl.contains("user_service") || lowerUrl.contains(":28081")) {
                log.error("CRITICAL CONFIGURATION ERROR: miningServiceUrl is incorrectly configured! URL: {}. " +
                         "It points to user-service instead of mining-service. " +
                         "Check MINING_SERVICE environment variable.", miningServiceUrl);
                throw new IllegalStateException(
                    String.format("Configuration error: miningServiceUrl is incorrectly set to user-service (%s) instead of mining-service. " +
                               "Check MINING_SERVICE environment variable. Expected: mining-service.boostx.svc.cluster.local:28083", 
                               miningServiceUrl));
            }
            
            String url = miningServiceUrl + "/api/v1/admin/mining/balances?page=" + pageable.getPageNumber() + "&size=" + pageable.getPageSize();
            log.debug("Calling mining service: {}", url);
            
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/admin/mining/balances")
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize())
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response == null) {
                log.warn("Mining service returned null response for listApBalances");
                return new PageImpl<>(List.of());
            }
            
            if (response.getSuccess() == null || !response.getSuccess()) {
                log.warn("Mining service returned unsuccessful response: {}", response.getMessage());
                return new PageImpl<>(List.of());
            }
            
            if (response.getData() == null) {
                log.warn("Mining service returned null data for listApBalances");
                return new PageImpl<>(List.of());
            }
            
            Map<String, Object> pageData = response.getData();
            log.debug("Received page data keys: {}", pageData.keySet());
            
            // Extract page information
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.get("content");
            Integer number = getIntegerValue(pageData, "number");
            Integer size = getIntegerValue(pageData, "size");
            Long totalElements = getLongValue(pageData, "totalElements");
            
            log.debug("Extracted: content size={}, number={}, size={}, totalElements={}", 
                    content != null ? content.size() : 0, number, size, totalElements);
            
            // Debug: Log first balance item if available
            if (content != null && !content.isEmpty()) {
                Map<String, Object> firstBalance = content.get(0);
                Object currentBalanceObj = firstBalance.get("currentBalance");
                log.info("First balance item - userId: {}, currentBalance: {} (type: {}, raw value: {}), all keys: {}", 
                        firstBalance.get("userId"),
                        currentBalanceObj,
                        currentBalanceObj != null ? currentBalanceObj.getClass().getSimpleName() : "null",
                        currentBalanceObj,
                        firstBalance.keySet());
                
                // Log all balance items for debugging
                for (int i = 0; i < Math.min(content.size(), 3); i++) {
                    Map<String, Object> balance = content.get(i);
                    log.info("Balance item {} - userId: {}, currentBalance: {}, monthlyAccumulated: {}, lifetimeAccumulated: {}", 
                            i, 
                            balance.get("userId"),
                            balance.get("currentBalance"),
                            balance.get("monthlyAccumulated"),
                            balance.get("lifetimeAccumulated"));
                }
            } else {
                log.warn("No balance content received from mining service! content is null or empty.");
            }
            
            if (content == null) {
                log.warn("Content is null in page data");
                content = List.of();
            }
            if (number == null) {
                number = 0;
            }
            if (size == null) {
                size = pageable.getPageSize();
            }
            if (totalElements == null) {
                totalElements = 0L;
            }
            
            return new PageImpl<>(
                    content,
                    PageRequest.of(number, size),
                    totalElements
            );
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // Connection refused or service unavailable - return empty page
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Mining-service is not available (connection refused/timeout). Returning empty AP balances list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Network error listing AP balances", e);
            throw new RuntimeException("Failed to list AP balances: " + e.getMessage(), e);
        } catch (WebClientResponseException.Forbidden e) {
            String requestUrl = e.getRequest() != null ? e.getRequest().getURI().toString() : "unknown";
            log.error("Access forbidden (403) when calling mining service. Request URL: {}, Response: {}", 
                     requestUrl, e.getResponseBodyAsString());
            
            // Check if we're accidentally calling the wrong service
            if (requestUrl.contains("user-service") || miningServiceUrl.contains("user-service")) {
                log.error("CRITICAL: MiningServiceRestClient is calling user-service instead of mining-service! " +
                         "miningServiceUrl={}, requestUrl={}. This is a configuration error. " +
                         "Check MINING_SERVICE environment variable.", miningServiceUrl, requestUrl);
                throw new IllegalStateException(
                    String.format("Configuration error: MiningServiceRestClient is calling user-service (%s) instead of mining-service. " +
                               "Check MINING_SERVICE environment variable. Current miningServiceUrl: %s", 
                               requestUrl, miningServiceUrl));
            }
            
            log.warn("Mining-service returned 403 Forbidden. Returning empty AP balances list. URL: {}", miningServiceUrl);
            return new PageImpl<>(List.of());
        } catch (WebClientResponseException e) {
            log.error("Error listing AP balances: status={}, message={}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to list AP balances: " + e.getMessage(), e);
        } catch (Exception e) {
            // Check if it's a RetryExhaustedException wrapping a Forbidden exception
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof WebClientResponseException.Forbidden) {
                    WebClientResponseException.Forbidden forbidden = (WebClientResponseException.Forbidden) cause;
                    String requestUrl = forbidden.getRequest() != null ? forbidden.getRequest().getURI().toString() : "unknown";
                    log.error("Access forbidden (403) when calling mining service (wrapped in RetryExhaustedException). Request URL: {}, Response: {}", 
                             requestUrl, forbidden.getResponseBodyAsString());
                    
                    // Check if we're accidentally calling the wrong service
                    if (requestUrl.contains("user-service") || miningServiceUrl.contains("user-service")) {
                        log.error("CRITICAL: MiningServiceRestClient is calling user-service instead of mining-service! " +
                                 "miningServiceUrl={}, requestUrl={}. This is a configuration error. " +
                                 "Check MINING_SERVICE environment variable.", miningServiceUrl, requestUrl);
                        throw new IllegalStateException(
                            String.format("Configuration error: MiningServiceRestClient is calling user-service (%s) instead of mining-service. " +
                                       "Check MINING_SERVICE environment variable. Current miningServiceUrl: %s", 
                                       requestUrl, miningServiceUrl));
                    }
                    
                    log.warn("Mining-service returned 403 Forbidden (after retries). Returning empty AP balances list. URL: {}", miningServiceUrl);
                    return new PageImpl<>(List.of());
                }
                cause = cause.getCause();
            }
            
            // Check if it's a connection error
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("Mining-service is not available. Returning empty AP balances list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Unexpected error listing AP balances", e);
            throw new RuntimeException("Failed to list AP balances: " + e.getMessage(), e);
        }
    }

    /**
     * Gets mining sessions for a user
     */
    public Page<Map<String, Object>> getUserMiningSessions(Long userId, Pageable pageable) {
        try {
            String url = miningServiceUrl + "/api/v1/admin/mining/users/" + userId + "/sessions?page=" + pageable.getPageNumber() + "&size=" + pageable.getPageSize();
            log.debug("Calling mining service: {}", url);
            
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/admin/mining/users/{userId}/sessions")
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize())
                            .build(userId))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response == null) {
                log.warn("Mining service returned null response for getUserMiningSessions userId={}", userId);
                return new PageImpl<>(List.of());
            }
            
            if (response.getSuccess() == null || !response.getSuccess()) {
                log.warn("Mining service returned unsuccessful response for userId={}: {}", userId, response.getMessage());
                return new PageImpl<>(List.of());
            }
            
            if (response.getData() == null) {
                log.warn("Mining service returned null data for getUserMiningSessions userId={}", userId);
                return new PageImpl<>(List.of());
            }
            
            Map<String, Object> pageData = response.getData();
            log.debug("Received page data keys for userId={}: {}", userId, pageData.keySet());
            
            // Extract page information
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.get("content");
            Integer number = getIntegerValue(pageData, "number");
            Integer size = getIntegerValue(pageData, "size");
            Long totalElements = getLongValue(pageData, "totalElements");
            
            log.debug("Extracted for userId={}: content size={}, number={}, size={}, totalElements={}", 
                    userId, content != null ? content.size() : 0, number, size, totalElements);
            
            if (content == null) {
                log.warn("Content is null in page data for userId={}", userId);
                content = List.of();
            }
            if (number == null) {
                number = 0;
            }
            if (size == null) {
                size = pageable.getPageSize();
            }
            if (totalElements == null) {
                totalElements = 0L;
            }
            
            return new PageImpl<>(
                    content,
                    PageRequest.of(number, size),
                    totalElements
            );
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // Connection refused or service unavailable - return empty page
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Mining-service is not available (connection refused/timeout). Returning empty mining sessions list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Network error getting mining sessions for user: {}", userId, e);
            throw new RuntimeException("Failed to get mining sessions: " + e.getMessage(), e);
        } catch (WebClientResponseException e) {
            log.error("Error getting mining sessions for user: {}, status={}, message={}", userId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to get mining sessions: " + e.getMessage(), e);
        } catch (Exception e) {
            // Check if it's a connection error
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("Mining-service is not available. Returning empty mining sessions list. URL: {}", miningServiceUrl);
                return new PageImpl<>(List.of());
            }
            log.error("Unexpected error getting mining sessions for user: {}", userId, e);
            throw new RuntimeException("Failed to get mining sessions: " + e.getMessage(), e);
        }
    }
    
    private Integer getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer value for key {}: {}", key, value);
            return null;
        }
    }

    /**
     * Gets comprehensive mining status for a user
     */
    public Map<String, Object> getMiningStatus(Long userId) {
        try {
            String url = miningServiceUrl + "/api/v1/mining/status";
            log.debug("Calling mining service: {} with X-User-Id: {}", url, userId);
            
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/v1/mining/status")
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response == null) {
                log.warn("Mining service returned null response for getMiningStatus userId={}", userId);
                return new java.util.HashMap<>();
            }
            
            if (response.getSuccess() == null || !response.getSuccess()) {
                log.warn("Mining service returned unsuccessful response for userId={}: {}", userId, response.getMessage());
                return new java.util.HashMap<>();
            }
            
            if (response.getData() == null) {
                log.warn("Mining service returned null data for getMiningStatus userId={}", userId);
                return new java.util.HashMap<>();
            }
            
            log.debug("Received mining status data for userId={}", userId);
            return response.getData();
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Mining-service is not available (connection refused/timeout). Returning empty status. URL: {}", miningServiceUrl);
                return new java.util.HashMap<>();
            }
            log.error("Network error getting mining status for user: {}", userId, e);
            throw new RuntimeException("Failed to get mining status: " + e.getMessage(), e);
        } catch (WebClientResponseException e) {
            log.error("Error getting mining status for user: {}, status={}, message={}", userId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to get mining status: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("Mining-service is not available. Returning empty status. URL: {}", miningServiceUrl);
                return new java.util.HashMap<>();
            }
            log.error("Unexpected error getting mining status for user: {}", userId, e);
            throw new RuntimeException("Failed to get mining status: " + e.getMessage(), e);
        }
    }

    /**
     * Gets AP balance for a user
     */
    public Map<String, Object> getUserApBalance(Long userId) {
        try {
            // Validate that we're using the correct service URL
            if (miningServiceUrl == null || miningServiceUrl.contains("token-service")) {
                log.error("CRITICAL: miningServiceUrl is incorrectly configured! URL: {}. Expected mining-service, not token-service!", miningServiceUrl);
                throw new IllegalStateException("Mining service URL is misconfigured. Expected mining-service, but got: " + miningServiceUrl);
            }
            
            String url = miningServiceUrl + "/api/v1/admin/mining/users/" + userId + "/balance";
            log.debug("Calling mining service: {} (miningServiceUrl={})", url, miningServiceUrl);
            
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/v1/admin/mining/users/{userId}/balance", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response == null) {
                log.warn("Mining service returned null response for getUserApBalance userId={}", userId);
                return new java.util.HashMap<>();
            }
            
            if (response.getSuccess() == null || !response.getSuccess()) {
                log.warn("Mining service returned unsuccessful response for userId={}: {}", userId, response.getMessage());
                return new java.util.HashMap<>();
            }
            
            if (response.getData() == null) {
                log.warn("Mining service returned null data for getUserApBalance userId={}", userId);
                return new java.util.HashMap<>();
            }
            
            log.debug("Received AP balance data for userId={}", userId);
            return response.getData();
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Mining-service is not available (connection refused/timeout). Returning empty balance. URL: {}", miningServiceUrl);
                return new java.util.HashMap<>();
            }
            log.error("Network error getting AP balance for user: {}", userId, e);
            throw new RuntimeException("Failed to get AP balance: " + e.getMessage(), e);
        } catch (WebClientResponseException e) {
            // Handle 500 errors gracefully - return empty map instead of throwing
            if (e.getStatusCode() != null && e.getStatusCode().is5xxServerError()) {
                log.warn("Mining service returned 5xx error for getUserApBalance userId={}, status={}, message={}. Returning empty balance.", 
                        userId, e.getStatusCode(), e.getResponseBodyAsString());
                return new java.util.HashMap<>();
            }
            log.error("Error getting AP balance for user: {}, status={}, message={}", userId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to get AP balance: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("Mining-service is not available. Returning empty balance. URL: {}", miningServiceUrl);
                return new java.util.HashMap<>();
            }
            log.error("Unexpected error getting AP balance for user: {}", userId, e);
            throw new RuntimeException("Failed to get AP balance: " + e.getMessage(), e);
        }
    }

    /**
     * Hard delete user (complete delete) from mining-service admin endpoint
     */
    public void hardDeleteUser(Long userId) {
        try {
            log.debug("Calling mining-service admin endpoint to hard delete user: userId={}", userId);
            getWebClient()
                    .delete()
                    .uri("/api/v1/admin/mining/users/{userId}/hard-delete", userId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("User hard deleted from mining-service successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling mining-service to hard delete user: userId={}", userId, e);
            throw new RuntimeException("Failed to hard delete user from mining-service: " + e.getMessage(), e);
        }
    }
}

