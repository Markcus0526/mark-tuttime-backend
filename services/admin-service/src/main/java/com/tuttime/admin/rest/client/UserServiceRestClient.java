package com.tuttime.admin.rest.client;

import com.tuttime.admin.dto.UpdateUserRequest;
import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.util.retry.Retry;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * REST Client for User Service
 * 
 * <p>Used for admin operations that require REST API calls to user-service.
 * Note: For admin operations like blocking/unblocking, we may need to add
 * admin-specific endpoints to user-service or use direct database access.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceRestClient {

    private final WebClient.Builder webClientBuilder;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @Qualifier("serviceCallExecutor")
    private Executor serviceCallExecutor;

    @Value("${services.user-service.url:http://localhost:28081}")
    private String userServiceUrl;

    @jakarta.annotation.PostConstruct
    public void validateConfiguration() {
        // Validate that the URL points to user-service, not mining-service or other services
        if (userServiceUrl == null || userServiceUrl.trim().isEmpty()) {
            log.error("CRITICAL CONFIGURATION ERROR: userServiceUrl is null or empty!");
            throw new IllegalStateException("userServiceUrl is not configured");
        }
        
        String lowerUrl = userServiceUrl.toLowerCase();
        if (lowerUrl.contains("mining-service")) {
            log.error("CRITICAL CONFIGURATION ERROR: userServiceUrl is incorrectly configured. It points to mining-service instead of user-service. URL: {}", userServiceUrl);
            throw new IllegalStateException("userServiceUrl is incorrectly configured. It points to mining-service instead of user-service. URL: " + userServiceUrl);
        }
        
        if (lowerUrl.contains("token-service")) {
            log.error("CRITICAL CONFIGURATION ERROR: userServiceUrl is incorrectly configured. It points to token-service instead of user-service. URL: {}", userServiceUrl);
            throw new IllegalStateException("userServiceUrl is incorrectly configured. It points to token-service instead of user-service. URL: " + userServiceUrl);
        }
        
        // Validate port (user-service should be on 28081)
        if (userServiceUrl.contains(":28083")) {
            log.error("CRITICAL CONFIGURATION ERROR: userServiceUrl uses port 28083 (mining-service port) instead of 28081 (user-service port)! URL: {}", userServiceUrl);
            throw new IllegalStateException("userServiceUrl uses wrong port (28083 is mining-service, should be 28081 for user-service). URL: " + userServiceUrl);
        }
        
        log.info("UserServiceRestClient configured with URL: {}", userServiceUrl);
    }

    private WebClient getWebClient() {
        return webClientBuilder.clone()
                .baseUrl(userServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Update user profile via REST API
     * Note: This requires admin authentication token to be passed
     * Note: This is a placeholder - actual implementation should use gRPC or proper DTOs
     */
    public Map<String, Object> updateUserProfile(Long userId, UpdateUserRequest request, String adminToken) {
        try {
            Map<String, Object> updateRequest = Map.of(
                    "firstName", request.getFirstName() != null ? request.getFirstName() : "",
                    "lastName", request.getLastName() != null ? request.getLastName() : "",
                    "email", request.getEmail() != null ? request.getEmail() : "",
                    "telegramId", request.getTelegram() != null ? request.getTelegram() : "",
                    "country", request.getCountry() != null ? request.getCountry() : "",
                    "city", request.getCity() != null ? request.getCity() : ""
            );

            ApiResponse<?> response = getWebClient()
                    .patch()
                    .uri("/api/v1/users/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .bodyValue(updateRequest)
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getData() instanceof Map) {
                return (Map<String, Object>) response.getData();
            }
            throw new RuntimeException("Failed to update user profile");
        } catch (Exception e) {
            log.error("REST error updating user profile: userId={}", userId, e);
            throw new RuntimeException("Failed to update user profile: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by ID via REST API (public endpoint)
     * Note: This is a placeholder - actual implementation should use gRPC
     */
    public Map<String, Object> getUserByIdPublic(Long userId) {
        try {
            ApiResponse<?> response = getWebClient()
                    .get()
                    .uri("/api/v1/users/{userId}", userId)
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getData() instanceof Map) {
                return (Map<String, Object>) response.getData();
            }
            return null;
        } catch (Exception e) {
            log.error("REST error getting user: userId={}", userId, e);
            return null;
        }
    }

    /**
     * Get device information by client device ID
     * 
     * @param clientDeviceId Client device ID (UUID from mobile app)
     * @return Map containing device information (userId, deviceId, etc.) or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDeviceByClientDeviceId(String clientDeviceId) {
        try {
            log.debug("Calling user-service to get device by clientDeviceId: {}", clientDeviceId);
            ApiResponse<?> response = getWebClient()
                    .get()
                    .uri("/api/v1/admin/devices/{clientDeviceId}", clientDeviceId)
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getData() instanceof Map) {
                return (Map<String, Object>) response.getData();
            }
            return null;
        } catch (Exception e) {
            log.error("REST error getting device by clientDeviceId: clientDeviceId={}", clientDeviceId, e);
            return null;
        }
    }

    /**
     * Update device status in user-service database
     * 
     * @param clientDeviceId Client device ID (UUID from mobile app)
     * @param isActive Whether the device should be active (null to skip)
     * @param isTrusted Whether the device should be trusted (null to skip)
     * @return Map containing updated device information or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateDeviceStatus(String clientDeviceId, Boolean isActive, Boolean isTrusted) {
        try {
            log.debug("Calling user-service to update device status: clientDeviceId={}, isActive={}, isTrusted={}", 
                    clientDeviceId, isActive, isTrusted);
            
            var uriBuilder = UriComponentsBuilder
                    .fromUriString(userServiceUrl + "/api/v1/admin/devices/{clientDeviceId}/status");
            
            if (isActive != null) {
                uriBuilder.queryParam("isActive", isActive);
            }
            if (isTrusted != null) {
                uriBuilder.queryParam("isTrusted", isTrusted);
            }
            
            ApiResponse<?> response = getWebClient()
                    .put()
                    .uri(uriBuilder.buildAndExpand(clientDeviceId).toUri())
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getData() instanceof Map) {
                return (Map<String, Object>) response.getData();
            }
            return null;
        } catch (Exception e) {
            log.error("REST error updating device status: clientDeviceId={}, isActive={}, isTrusted={}", 
                    clientDeviceId, isActive, isTrusted, e);
            return null;
        }
    }

    /**
     * Get all available languages from user-service
     *
     * @return Set of language codes
     */
    public Set<String> getAllLanguages() {
        try {
            log.info("Calling user-service to get all languages: {}/api/v1/users/languages", userServiceUrl);
            ApiResponse<Set<String>> response = getWebClient()
                    .get()
                    .uri("/api/v1/users/languages")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Set<String>>>() {})
                    .block();

            if (response != null) {
                log.info("Response from user-service: success={}, data={}", response.getSuccess(), response.getData());
                if (response.getData() != null) {
                    log.info("Retrieved {} languages from user-service: {}", response.getData().size(), response.getData());
                    return response.getData();
                } else {
                    log.warn("Response data is null from user-service. Response: {}", response);
                    return Set.of();
                }
            } else {
                log.error("Null response from user-service");
                throw new RuntimeException("Null response from user-service");
            }
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // Connection refused or service unavailable - return empty set
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("User-service is not available (connection refused/timeout). Returning empty languages set. URL: {}", userServiceUrl);
                return Set.of();
            }
            log.error("Network error calling user-service to get languages", e);
            throw new RuntimeException("Failed to get languages: " + e.getMessage(), e);
        } catch (WebClientResponseException e) {
            log.error("Error calling user-service to get languages: status={}, message={}, body={}",
                    e.getStatusCode(), e.getMessage(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get languages: " + e.getMessage() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            // Check if it's a connection error
            if (e.getCause() instanceof java.net.ConnectException || 
                e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                                           e.getMessage().contains("Connection timed out"))) {
                log.warn("User-service is not available. Returning empty languages set. URL: {}", userServiceUrl);
                return Set.of();
            }
            log.error("Unexpected error getting languages", e);
            throw new RuntimeException("Failed to get languages: " + e.getMessage(), e);
        }
    }

    /**
     * Get language translations by code from user-service
     *
     * @param code Language code (e.g., "en", "ko")
     * @return Map of translations
     */
    public Map<String, Object> getLanguageByCode(String code) {
        try {
            log.debug("Calling user-service to get language by code: {} from {}", code, userServiceUrl);
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/v1/users/languages/{code}", code)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();

            if (response != null && response.getData() != null) {
                log.debug("Retrieved language {} from user-service", code);
                return response.getData();
            } else {
                log.warn("Language not found in user-service: {}", code);
                throw new RuntimeException("Language not found: " + code);
            }
        } catch (WebClientResponseException.NotFound e) {
            log.error("Language not found in user-service: code={}", code);
            throw new RuntimeException("Language not found: " + code, e);
        } catch (WebClientResponseException e) {
            log.error("Error calling user-service to get language: status={}, message={}",
                    e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to get language: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error getting language", e);
            throw new RuntimeException("Failed to get language: " + e.getMessage(), e);
        }
    }

    /**
     * Upload/update language translations in user-service
     *
     * @param code Language code
     * @param translations Map of translation key-value pairs
     */
    public void uploadTranslations(String code, Map<String, Object> translations) {
        try {
            log.debug("Calling user-service to upload translations for language: {}", code);
            getWebClient()
                    .post()
                    .uri("/api/v1/users/languages/{code}", code)
                    .bodyValue(translations)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Translations uploaded successfully for language: {}", code);
        } catch (WebClientResponseException e) {
            log.error("Error calling user-service to upload translations: status={}, message={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to upload translations: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error uploading translations", e);
            throw new RuntimeException("Failed to upload translations: " + e.getMessage(), e);
        }
    }

    /**
     * Update language translations in user-service
     *
     * @param code Language code
     * @param translations Map of translation key-value pairs
     */
    public void updateTranslations(String code, Map<String, Object> translations) {
        try {
            log.debug("Calling user-service to update translations for language: {}", code);
            getWebClient()
                    .put()
                    .uri("/api/v1/users/languages/{code}", code)
                    .bodyValue(translations)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Translations updated successfully for language: {}", code);
        } catch (WebClientResponseException e) {
            log.error("Error calling user-service to update translations: status={}, message={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to update translations: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error updating translations", e);
            throw new RuntimeException("Failed to update translations: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a language from user-service
     *
     * @param code Language code to delete
     */
    public void deleteLanguage(String code) {
        try {
            log.debug("Calling user-service to delete language: {}", code);
            getWebClient()
                    .delete()
                    .uri("/api/v1/users/languages/{code}", code)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Language deleted successfully: {}", code);
        } catch (WebClientResponseException.NotFound e) {
            log.error("Language not found in user-service: code={}", code);
            throw new RuntimeException("Language not found: " + code, e);
        } catch (WebClientResponseException e) {
            log.error("Error calling user-service to delete language: status={}, message={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to delete language: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error deleting language", e);
            throw new RuntimeException("Failed to delete language: " + e.getMessage(), e);
        }
    }

    // ==================== Admin User Management Methods ====================

    /**
     * List users with search and filters from user-service admin endpoint
     * Returns Map representation of Page<UserResponse> since we can't access user-service DTOs directly
     */
    public Map<String, Object> listUsers(
            String search, String status, String kycStatus, 
            boolean includeDeleted,
            org.springframework.data.domain.Pageable pageable) {
        try {
            log.debug("Calling user-service admin endpoint to list users: search={}, status={}, kycStatus={}, includeDeleted={}", 
                    search, status, kycStatus, includeDeleted);
            
            StringBuilder uriBuilder = new StringBuilder("/api/v1/admin/users");
            boolean hasParams = false;
            if (search != null && !search.trim().isEmpty()) {
                // URL encode search parameter to handle special characters
                String encodedSearch = java.net.URLEncoder.encode(search.trim(), java.nio.charset.StandardCharsets.UTF_8);
                uriBuilder.append(hasParams ? "&" : "?").append("search=").append(encodedSearch);
                hasParams = true;
            }
            if (status != null && !status.trim().isEmpty()) {
                uriBuilder.append(hasParams ? "&" : "?").append("status=").append(status.trim());
                hasParams = true;
            }
            if (kycStatus != null && !kycStatus.trim().isEmpty()) {
                uriBuilder.append(hasParams ? "&" : "?").append("kycStatus=").append(kycStatus.trim());
                hasParams = true;
            }
            if (includeDeleted) {
                uriBuilder.append(hasParams ? "&" : "?").append("includeDeleted=true");
                hasParams = true;
            }
            if (pageable != null) {
                uriBuilder.append(hasParams ? "&" : "?")
                        .append("page=").append(pageable.getPageNumber())
                        .append("&size=").append(pageable.getPageSize());
                if (pageable.getSort().isSorted()) {
                    pageable.getSort().forEach(order -> {
                        uriBuilder.append("&sort=").append(order.getProperty())
                                .append(",").append(order.getDirection().name().toLowerCase());
                    });
                }
            } else {
                // Default pagination if not provided
                uriBuilder.append(hasParams ? "&" : "?")
                        .append("page=0&size=100");
            }
            
            // Always ensure size is set (minimum 1 to avoid empty pages)
            if (!uriBuilder.toString().contains("size=")) {
                uriBuilder.append(hasParams ? "&" : "?")
                        .append("size=100");
            }
            
            String fullUrl = userServiceUrl + uriBuilder.toString();
            log.info("Calling user-service admin endpoint: {}", fullUrl);
            
            try {
                // Execute blocking call on a non-reactive executor to avoid reactor thread issues
                ApiResponse<Map<String, Object>> response;
                if (serviceCallExecutor != null) {
                    // Use executor to run blocking call on non-reactive thread
                    CompletableFuture<ApiResponse<Map<String, Object>>> future = CompletableFuture.supplyAsync(() -> {
                        return getWebClient()
                                .get()
                                .uri(uriBuilder.toString())
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.isError(), clientResponse -> {
                                    try {
                                        String errorBody = clientResponse.bodyToMono(String.class)
                                                .block(java.time.Duration.ofSeconds(2));
                                        log.error("Error response from user-service: status={}, body={}", 
                                                clientResponse.statusCode(), errorBody);
                                    } catch (Exception e) {
                                        log.error("Error reading error response body: status={}", 
                                                clientResponse.statusCode(), e);
                                    }
                                    return clientResponse.createException();
                                })
                                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                                .block(java.time.Duration.ofSeconds(10));
                    }, serviceCallExecutor);
                    
                    response = future.get(15, TimeUnit.SECONDS);
                } else {
                    // Fallback if executor is not available (shouldn't happen in production)
                    log.warn("serviceCallExecutor not available, using direct block (may cause issues in reactive context)");
                    response = getWebClient()
                            .get()
                            .uri(uriBuilder.toString())
                            .retrieve()
                            .onStatus(httpStatus -> httpStatus.isError(), clientResponse -> {
                                try {
                                    String errorBody = clientResponse.bodyToMono(String.class)
                                            .block(java.time.Duration.ofSeconds(2));
                                    log.error("Error response from user-service: status={}, body={}", 
                                            clientResponse.statusCode(), errorBody);
                                } catch (Exception e) {
                                    log.error("Error reading error response body: status={}", 
                                            clientResponse.statusCode(), e);
                                }
                                return clientResponse.createException();
                            })
                            .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                            .block(java.time.Duration.ofSeconds(10));
                }
                
                if (response == null) {
                    log.warn("Null response from user-service");
                    throw new RuntimeException("Null response from user-service");
                }
                
                log.info("Response from user-service: success={}, message={}, data type={}", 
                        response.getSuccess(), 
                        response.getMessage(),
                        response.getData() != null ? response.getData().getClass().getName() : "null");
                
                if (response.getData() != null) {
                    // The data is a Page object serialized as Map
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pageData = (Map<String, Object>) response.getData();
                    log.info("Page data from user-service - keys: {}, totalElements: {}, totalPages: {}, number: {}, size: {}, contentSize: {}", 
                            pageData.keySet(), 
                            pageData.get("totalElements"),
                            pageData.get("totalPages"),
                            pageData.get("number"),
                            pageData.get("size"),
                            pageData.get("content") != null ? ((List<?>) pageData.get("content")).size() : 0);
                    return pageData;
                } else {
                    log.warn("Response data is null from user-service");
                }
            } catch (TimeoutException e) {
                log.error("Timeout calling user-service: {}", fullUrl, e);
                throw new RuntimeException("Timeout calling user-service: " + e.getMessage(), e);
            } catch (java.util.concurrent.ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                    org.springframework.web.reactive.function.client.WebClientResponseException wce = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) cause;
                    log.error("WebClient error calling user-service: URL={}, status={}, body={}", 
                            fullUrl, wce.getStatusCode(), wce.getResponseBodyAsString(), wce);
                    throw new RuntimeException("Failed to call user-service: " + wce.getMessage() + " (URL: " + fullUrl + ")", wce);
                }
                log.error("Execution error calling user-service: URL={}", fullUrl, cause);
                throw new RuntimeException("Failed to call user-service: " + cause.getMessage() + " (URL: " + fullUrl + ")", cause);
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                log.error("WebClient error calling user-service: URL={}, status={}, body={}", 
                        fullUrl, e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException("Failed to call user-service: " + e.getMessage() + " (URL: " + fullUrl + ")", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted calling user-service: {}", fullUrl, e);
                throw new RuntimeException("Interrupted calling user-service: " + e.getMessage(), e);
            }
            Map<String, Object> emptyPage = new java.util.HashMap<>();
            emptyPage.put("content", new java.util.ArrayList<>());
            emptyPage.put("totalElements", 0L);
            emptyPage.put("totalPages", 0);
            emptyPage.put("number", 0);
            emptyPage.put("size", 20);
            return emptyPage;
        } catch (Exception e) {
            log.error("Error calling user-service to list users", e);
            throw new RuntimeException("Failed to list users: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by ID from user-service admin endpoint
     * Returns Map representation of UserResponse since we can't access user-service DTOs directly
     */
    public Map<String, Object> getUserById(Long userId, boolean includeDeleted) {
        try {
            log.debug("Calling user-service admin endpoint to get user: userId={}, includeDeleted={}", userId, includeDeleted);
            final String uri = includeDeleted 
                    ? "/api/v1/admin/users/{userId}?includeDeleted=true"
                    : "/api/v1/admin/users/{userId}";
            
            // Execute blocking call on a non-reactive executor to avoid reactor thread issues
            ApiResponse<Map<String, Object>> response;
            if (serviceCallExecutor != null) {
                CompletableFuture<ApiResponse<Map<String, Object>>> future = CompletableFuture.supplyAsync(() -> {
                    return getWebClient()
                            .get()
                            .uri(uri, userId)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                            .block(java.time.Duration.ofSeconds(10));
                }, serviceCallExecutor);
                
                response = future.get(15, TimeUnit.SECONDS);
            } else {
                // Fallback if executor is not available
                log.warn("serviceCallExecutor not available, using direct block (may cause issues in reactive context)");
                response = getWebClient()
                        .get()
                        .uri(uri, userId)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                        .block(java.time.Duration.ofSeconds(10));
            }
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            throw new RuntimeException("User not found: " + userId);
        } catch (TimeoutException e) {
            log.error("Timeout calling user-service to get user: userId={}", userId, e);
            throw new RuntimeException("Timeout getting user: " + userId, e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof WebClientResponseException.NotFound) {
                throw new RuntimeException("User not found: " + userId, cause);
            }
            log.error("Execution error calling user-service to get user: userId={}", userId, cause);
            throw new RuntimeException("Failed to get user: " + cause.getMessage(), cause);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("User not found: " + userId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted calling user-service to get user: userId={}", userId, e);
            throw new RuntimeException("Interrupted getting user: " + userId, e);
        } catch (Exception e) {
            log.error("Error calling user-service to get user: userId={}", userId, e);
            throw new RuntimeException("Failed to get user: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get user by ID from user-service admin endpoint (without includeDeleted parameter, defaults to false)
     */
    public Map<String, Object> getUserById(Long userId) {
        return getUserById(userId, false);
    }

    /**
     * Update user from user-service admin endpoint
     * Returns Map representation of UserResponse since we can't access user-service DTOs directly
     */
    public Map<String, Object> updateUser(Long userId, UpdateUserRequest request) {
        try {
            log.debug("Calling user-service admin endpoint to update user: userId={}", userId);
            // Convert UpdateUserRequest to Map for user-service
            // Note: null means "not provided / skip", so only include fields that are non-null
            java.util.Map<String, Object> requestMap = new java.util.HashMap<>();
            if (request.getFirstName() != null) requestMap.put("firstName", request.getFirstName());
            if (request.getLastName() != null) requestMap.put("lastName", request.getLastName());
            if (request.getUsername() != null) requestMap.put("username", request.getUsername());
            if (request.getEmail() != null) requestMap.put("email", request.getEmail());
            if (request.getPhone() != null) requestMap.put("phone", request.getPhone());
            if (request.getTelegram() != null) requestMap.put("telegramId", request.getTelegram());
            if (request.getCountry() != null) requestMap.put("country", request.getCountry());
            if (request.getCity() != null) requestMap.put("city", request.getCity());
            if (request.getDateOfBirth() != null) requestMap.put("dateOfBirth", request.getDateOfBirth());
            if (request.getGender() != null) requestMap.put("gender", request.getGender());
            if (request.getInstagram() != null) requestMap.put("instagram", request.getInstagram());
            
            Map<String, Object> requestBody = requestMap;
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .put()
                    .uri("/api/v1/admin/users/{userId}", userId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            throw new BusinessException("Failed to update user: " + userId, "USER_UPDATE_FAILED");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling user-service to update user: userId={}", userId, e);
            throw new BusinessException("Failed to update user: " + e.getMessage(), "USER_UPDATE_FAILED");
        }
    }

    /**
     * Block user from user-service admin endpoint
     * Returns Map representation of UserResponse since we can't access user-service DTOs directly
     */
    public Map<String, Object> blockUser(Long userId, String reason) {
        try {
            log.debug("Calling user-service admin endpoint to block user: userId={}, reason={}", userId, reason);
            Map<String, String> requestBody = Map.of("reason", reason != null ? reason : "");
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/block", userId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            throw new RuntimeException("Failed to block user: " + userId);
        } catch (Exception e) {
            log.error("Error calling user-service to block user: userId={}", userId, e);
            throw new RuntimeException("Failed to block user: " + e.getMessage(), e);
        }
    }

    /**
     * Unblock user from user-service admin endpoint
     * Returns Map representation of UserResponse since we can't access user-service DTOs directly
     */
    public Map<String, Object> unblockUser(Long userId) {
        try {
            log.debug("Calling user-service admin endpoint to unblock user: userId={}", userId);
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/unblock", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            throw new RuntimeException("Failed to unblock user: " + userId);
        } catch (Exception e) {
            log.error("Error calling user-service to unblock user: userId={}", userId, e);
            throw new RuntimeException("Failed to unblock user: " + e.getMessage(), e);
        }
    }

    /**
     * Delete user (soft delete) from user-service admin endpoint
     */
    public void deleteUser(Long userId) {
        try {
            log.debug("Calling user-service admin endpoint to delete user: userId={}", userId);
            getWebClient()
                    .delete()
                    .uri("/api/v1/admin/users/{userId}", userId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("User deleted successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to delete user: userId={}", userId, e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    /**
     * Get user referrals from user-service admin endpoint
     * Returns Map representation since we can't access user-service DTOs directly
     */
    public Map<String, Object> getUserReferrals(Long userId) {
        try {
            log.debug("Calling user-service admin endpoint to get user referrals: userId={}", userId);
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/v1/admin/users/{userId}/referrals", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            throw new RuntimeException("Failed to get user referrals: " + userId);
        } catch (Exception e) {
            log.error("Error calling user-service to get user referrals: userId={}", userId, e);
            throw new RuntimeException("Failed to get user referrals: " + e.getMessage(), e);
        }
    }

    /**
     * Get blocked devices from user-service admin endpoint
     * Returns Map representation of Page since we can't access user-service DTOs directly
     * 
     * @param pageable Pagination parameters
     * @return Map containing page data with blocked devices
     */
    public Map<String, Object> getBlockedDevices(org.springframework.data.domain.Pageable pageable) {
        try {
            log.debug("Calling user-service to get blocked devices: page={}, size={}", 
                    pageable.getPageNumber(), pageable.getPageSize());
            
            StringBuilder uriBuilder = new StringBuilder("/api/v1/admin/devices/blocked");
            uriBuilder.append("?page=").append(pageable.getPageNumber())
                    .append("&size=").append(pageable.getPageSize());
            if (pageable.getSort().isSorted()) {
                pageable.getSort().forEach(order -> {
                    uriBuilder.append("&sort=").append(order.getProperty())
                            .append(",").append(order.getDirection().name().toLowerCase());
                });
            }
            
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder.toString())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && response.getData() != null) {
                return response.getData();
            }
            
            // Return empty page if no data
            Map<String, Object> emptyPage = new java.util.HashMap<>();
            emptyPage.put("content", new java.util.ArrayList<>());
            emptyPage.put("totalElements", 0L);
            emptyPage.put("totalPages", 0);
            emptyPage.put("number", 0);
            emptyPage.put("size", pageable.getPageSize());
            return emptyPage;
        } catch (Exception e) {
            log.error("REST error getting blocked devices: page={}, size={}", 
                    pageable.getPageNumber(), pageable.getPageSize(), e);
            // Return empty page on error
            Map<String, Object> emptyPage = new java.util.HashMap<>();
            emptyPage.put("content", new java.util.ArrayList<>());
            emptyPage.put("totalElements", 0L);
            emptyPage.put("totalPages", 0);
            emptyPage.put("number", 0);
            emptyPage.put("size", pageable.getPageSize());
            return emptyPage;
        }
    }

    /**
     * Get devices by IP address from user-service admin endpoint
     * Returns List of device maps
     * 
     * @param ipAddress IP address to search for
     * @return List of devices with the specified IP address
     */
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> getDevicesByIpAddress(String ipAddress) {
        try {
            log.debug("Calling user-service to get devices by IP address: {}", ipAddress);
            ApiResponse<java.util.List<Map<String, Object>>> response = getWebClient()
                    .get()
                    .uri("/api/v1/admin/devices/by-ip/{ipAddress}", ipAddress)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<java.util.List<Map<String, Object>>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .block();

            if (response != null && response.getData() != null) {
                return response.getData();
            }
            return java.util.Collections.emptyList();
        } catch (Exception e) {
            log.error("REST error getting devices by IP address: ipAddress={}", ipAddress, e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get user overview from user-service admin endpoint
     * Returns comprehensive user overview with all data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserOverview(Long userId, 
                                                Integer conversionPage, Integer conversionSize,
                                                Integer transactionPage, Integer transactionSize,
                                                Integer boostPage, Integer boostSize) {
        try {
            log.debug("Calling user-service admin endpoint to get user overview: userId={}", userId);
            
            StringBuilder uriBuilder = new StringBuilder("/api/v1/admin/users/{userId}/overview");
            boolean hasParams = false;
            if (conversionPage != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("conversionPage=").append(conversionPage);
                hasParams = true;
            }
            if (conversionSize != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("conversionSize=").append(conversionSize);
                hasParams = true;
            }
            if (transactionPage != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("transactionPage=").append(transactionPage);
                hasParams = true;
            }
            if (transactionSize != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("transactionSize=").append(transactionSize);
                hasParams = true;
            }
            if (boostPage != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("boostPage=").append(boostPage);
                hasParams = true;
            }
            if (boostSize != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("boostSize=").append(boostSize);
                hasParams = true;
            }
            
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder.toString(), userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            throw new RuntimeException("Failed to get user overview: " + userId);
        } catch (Exception e) {
            log.error("Error calling user-service to get user overview: userId={}", userId, e);
            throw new RuntimeException("Failed to get user overview: " + e.getMessage(), e);
        }
    }

    /**
     * Get extended user detail from user-service admin endpoint
     * Returns user detail without full history
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getExtendedUserDetail(Long userId) {
        try {
            log.debug("Calling user-service admin endpoint to get extended user detail: userId={}", userId);
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/v1/admin/users/{userId}/detail", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            throw new RuntimeException("Failed to get extended user detail: " + userId);
        } catch (Exception e) {
            log.error("Error calling user-service to get extended user detail: userId={}", userId, e);
            throw new RuntimeException("Failed to get extended user detail: " + e.getMessage(), e);
        }
    }

    /**
     * Reset user mining via user-service admin endpoint
     */
    public void resetMining(Long userId, Map<String, Object> request) {
        try {
            log.debug("Calling user-service admin endpoint to reset mining: userId={}", userId);
            getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/actions/reset-mining", userId)
                    .bodyValue(request != null ? request : Map.of())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Mining reset successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to reset mining: userId={}", userId, e);
            throw new RuntimeException("Failed to reset mining: " + e.getMessage(), e);
        }
    }

    /**
     * Pause user account via user-service admin endpoint
     */
    public void pauseAccount(Long userId, Map<String, Object> request) {
        try {
            log.debug("Calling user-service admin endpoint to pause account: userId={}", userId);
            getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/actions/pause", userId)
                    .bodyValue(request != null ? request : Map.of())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Account paused successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to pause account: userId={}", userId, e);
            throw new RuntimeException("Failed to pause account: " + e.getMessage(), e);
        }
    }

    /**
     * Unpause user account via user-service admin endpoint
     */
    public void unpauseAccount(Long userId) {
        try {
            log.debug("Calling user-service admin endpoint to unpause account: userId={}", userId);
            getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/actions/unpause", userId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Account unpaused successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to unpause account: userId={}", userId, e);
            throw new RuntimeException("Failed to unpause account: " + e.getMessage(), e);
        }
    }

    /**
     * Force KYC verification via user-service admin endpoint
     */
    public void forceKyc(Long userId, Map<String, Object> request) {
        try {
            log.debug("Calling user-service admin endpoint to force KYC: userId={}", userId);
            getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/actions/force-kyc", userId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(java.time.Duration.ofSeconds(10));
            log.info("KYC status updated successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to force KYC: userId={}", userId, e);
            throw new RuntimeException("Failed to force KYC: " + e.getMessage(), e);
        }
    }

    /**
     * Add bonus to user account via user-service admin endpoint
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> addBonus(Long userId, Map<String, Object> request) {
        try {
            log.debug("Calling user-service admin endpoint to add bonus: userId={}", userId);
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/actions/add-bonus", userId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block(java.time.Duration.ofSeconds(10));
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            throw new RuntimeException("Failed to add bonus: " + userId);
        } catch (Exception e) {
            log.error("Error calling user-service to add bonus: userId={}", userId, e);
            throw new RuntimeException("Failed to add bonus: " + e.getMessage(), e);
        }
    }

    /**
     * Freeze user withdrawals via user-service admin endpoint
     */
    public void freezeWithdrawal(Long userId, Map<String, Object> request) {
        try {
            log.debug("Calling user-service admin endpoint to freeze withdrawal: userId={}", userId);
            getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/actions/freeze-withdrawal", userId)
                    .bodyValue(request != null ? request : Map.of())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Withdrawal frozen successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to freeze withdrawal: userId={}", userId, e);
            throw new RuntimeException("Failed to freeze withdrawal: " + e.getMessage(), e);
        }
    }

    /**
     * Unfreeze user withdrawals via user-service admin endpoint
     */
    public void unfreezeWithdrawal(Long userId) {
        try {
            log.debug("Calling user-service admin endpoint to unfreeze withdrawal: userId={}", userId);
            getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/actions/unfreeze-withdrawal", userId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Withdrawal unfrozen successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to unfreeze withdrawal: userId={}", userId, e);
            throw new RuntimeException("Failed to unfreeze withdrawal: " + e.getMessage(), e);
        }
    }

    /**
     * Mark user for anti-fraud watch via user-service admin endpoint
     */
    public void markAntiFraud(Long userId, Map<String, Object> request) {
        try {
            log.debug("Calling user-service admin endpoint to mark anti-fraud: userId={}", userId);
            getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/actions/mark-anti-fraud", userId)
                    .bodyValue(request != null ? request : Map.of())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("User marked for anti-fraud watch successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to mark anti-fraud: userId={}", userId, e);
            throw new RuntimeException("Failed to mark anti-fraud: " + e.getMessage(), e);
        }
    }

    /**
     * Unmark user from anti-fraud watch via user-service admin endpoint
     */
    public void unmarkAntiFraud(Long userId) {
        try {
            log.debug("Calling user-service admin endpoint to unmark anti-fraud: userId={}", userId);
            getWebClient()
                    .post()
                    .uri("/api/v1/admin/users/{userId}/actions/unmark-anti-fraud", userId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Anti-fraud watch removed successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to unmark anti-fraud: userId={}", userId, e);
            throw new RuntimeException("Failed to unmark anti-fraud: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by phone number from user-service admin endpoint
     * Returns Map representation of UserResponse since we can't access user-service DTOs directly
     * 
     * Note: Manually builds URI to avoid double-encoding issues with special characters like '+'
     * WebClient's path variable expansion encodes values, so we build the URI manually with proper encoding
     */
    public Map<String, Object> getUserByPhoneNumber(String phoneNumber) {
        try {
            log.debug("Calling user-service admin endpoint to get user by phone: phoneNumber={}", phoneNumber);
            
            // Decode first in case it's already encoded, then encode properly once
            String decodedPhone = phoneNumber;
            try {
                decodedPhone = java.net.URLDecoder.decode(phoneNumber, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                // If decoding fails, assume it's not encoded and use as-is
                decodedPhone = phoneNumber;
            }
            
            // Encode the phone number properly for URL path (only encode once)
            // Use UriUtils to ensure proper encoding
            String encodedPhone = UriUtils.encodePath(decodedPhone, java.nio.charset.StandardCharsets.UTF_8);
            String uri = "/api/v1/admin/users/by-phone/" + encodedPhone;
            
            log.debug("Encoded phone number for URI: original={}, decoded={}, encoded={}, finalUri={}", 
                    phoneNumber, decodedPhone, encodedPhone, uri);
            
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            throw new RuntimeException("User not found with phone number: " + phoneNumber);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("User not found with phone number: " + phoneNumber, e);
        } catch (WebClientResponseException.Forbidden e) {
            String uri = "/api/v1/admin/users/by-phone/" + UriUtils.encodePath(
                    phoneNumber.startsWith("%") ? java.net.URLDecoder.decode(phoneNumber, java.nio.charset.StandardCharsets.UTF_8) : phoneNumber,
                    java.nio.charset.StandardCharsets.UTF_8);
            log.error("403 Forbidden when calling user-service to get user by phone: phoneNumber={}, uri={}, response={}", 
                    phoneNumber, uri, e.getResponseBodyAsString(), e);
            throw new RuntimeException("Access denied when getting user by phone. Endpoint may require authentication.", e);
        } catch (Exception e) {
            log.error("Error calling user-service to get user by phone: phoneNumber={}", phoneNumber, e);
            throw new RuntimeException("Failed to get user by phone: " + e.getMessage(), e);
        }
    }

    /**
     * Hard delete user (complete delete, not soft delete) from user-service admin endpoint
     */
    public void hardDeleteUser(Long userId) {
        try {
            log.debug("Calling user-service admin endpoint to hard delete user: userId={}", userId);
            getWebClient()
                    .delete()
                    .uri("/api/v1/admin/users/{userId}/hard-delete", userId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("User hard deleted successfully: userId={}", userId);
        } catch (Exception e) {
            log.error("Error calling user-service to hard delete user: userId={}", userId, e);
            throw new RuntimeException("Failed to hard delete user: " + e.getMessage(), e);
        }
    }

    /**
     * Get blocked users from user-service admin endpoint
     * Returns Map representation of Page since we can't access user-service DTOs directly
     */
    public Map<String, Object> getBlockedUsers(org.springframework.data.domain.Pageable pageable) {
        try {
            log.debug("Calling user-service admin endpoint to get blocked users");
            StringBuilder uriBuilder = new StringBuilder("/api/v1/admin/users/blocklist");
            if (pageable != null) {
                uriBuilder.append("?page=").append(pageable.getPageNumber())
                        .append("&size=").append(pageable.getPageSize());
            } else {
                uriBuilder.append("?page=0&size=50");
            }
            
            String fullUrl = userServiceUrl + uriBuilder.toString();
            log.info("Calling user-service admin endpoint to get blocked users: {}", fullUrl);
            
            ApiResponse<Map<String, Object>> response = 
                    getWebClient()
                    .get()
                    .uri(uriBuilder.toString())
                    .retrieve()
                    .onStatus(httpStatus -> httpStatus.isError(), clientResponse -> {
                        String errorBody = clientResponse.bodyToMono(String.class).block();
                        log.error("Error response from user-service when getting blocked users: status={}, body={}", 
                                clientResponse.statusCode(), errorBody);
                        return clientResponse.createException();
                    })
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .block();
            
            if (response != null && response.getData() != null) {
                log.debug("Successfully retrieved blocked users from user-service: totalElements={}", 
                        response.getData().getOrDefault("totalElements", 0));
                return response.getData();
            }
            
            log.warn("Response from user-service was null or had no data, returning empty page");
            Map<String, Object> emptyPage = new java.util.HashMap<>();
            emptyPage.put("content", new java.util.ArrayList<>());
            emptyPage.put("totalElements", 0L);
            emptyPage.put("totalPages", 0);
            emptyPage.put("number", 0);
            emptyPage.put("size", pageable != null ? pageable.getPageSize() : 50);
            return emptyPage;
        } catch (WebClientResponseException e) {
            log.error("WebClient error calling user-service to get blocked users: status={}, body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to get blocked users from user-service: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling user-service to get blocked users", e);
            throw new RuntimeException("Failed to get blocked users: " + e.getMessage(), e);
        }
    }
}

