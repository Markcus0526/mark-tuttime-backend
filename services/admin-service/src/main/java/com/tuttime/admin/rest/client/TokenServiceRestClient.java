package com.tuttime.admin.rest.client;

import com.tuttime.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * REST Client for Token Service
 *
 * <p>Used for admin operations that require REST API calls to token-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenServiceRestClient {

	private final WebClient.Builder webClientBuilder;

	@Value("${services.token-service.url:${TOKEN_SERVICE_URL:http://localhost:28082}}")
	private String tokenServiceUrl;

	private WebClient getWebClient() {
		return webClientBuilder.clone()
				.baseUrl(tokenServiceUrl)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	/**
	 * Get all status levels from token-service
	 *
	 * @return List of status levels as Map (to avoid DTO class conflicts)
	 */
	public List<Map<String, Object>> getAllStatusLevels() {
		try {
			log.debug("Calling token-service to get all status levels: {}", tokenServiceUrl);
			ApiResponse<List<Map<String, Object>>> response = getWebClient()
					.get()
					.uri("/api/v1/status-levels")
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
					})
					.timeout(java.time.Duration.ofSeconds(5))
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Retrieved {} status levels from token-service", response.getData().size());
				return response.getData();
			} else {
				log.warn("No status levels returned from token-service");
				return List.of();
			}
		} catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
			// Connection refused or service unavailable - log as warning, not error
			if (e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
					e.getMessage().contains("Connection timed out") ||
					e.getMessage().contains("Service Unavailable"))) {
				log.warn("Token-service is not available (connection refused/timeout). Returning empty status levels list. URL: {}", tokenServiceUrl);
				return List.of();
			}
			log.error("Network error calling token-service to get status levels: {}", e.getMessage());
			throw new RuntimeException("Failed to get status levels: " + e.getMessage(), e);
		} catch (WebClientResponseException e) {
			log.error("Error calling token-service to get status levels: status={}, message={}",
					e.getStatusCode(), e.getMessage());
			throw new RuntimeException("Failed to get status levels: " + e.getMessage(), e);
		} catch (Exception e) {
			// Check if it's a connection error
			if (e.getCause() instanceof java.net.ConnectException ||
					e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
							e.getMessage().contains("Connection timed out"))) {
				log.warn("Token-service is not available. Returning empty status levels list. URL: {}", tokenServiceUrl);
				return List.of();
			}
			log.error("Unexpected error getting status levels", e);
			throw new RuntimeException("Failed to get status levels: " + e.getMessage(), e);
		}
	}

	/**
	 * Get a status level by name from token-service
	 *
	 * @param levelName The level name (e.g., "SILVER", "GOLD")
	 * @return Status level as Map (to avoid DTO class conflicts)
	 */
	public Map<String, Object> getStatusLevelByLevelName(String levelName) {
		try {
			log.debug("Calling token-service to get status level by name: {} from {}", levelName, tokenServiceUrl);
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/v1/status-levels/{level}", levelName)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Retrieved status level {} from token-service", levelName);
				return response.getData();
			} else {
				log.warn("No status level returned from token-service for levelName: {}", levelName);
				throw new RuntimeException("Status level not found: " + levelName);
			}
		} catch (WebClientResponseException.NotFound e) {
			log.error("Status level not found in token-service: levelName={}", levelName);
			throw new RuntimeException("Status level not found: " + levelName, e);
		} catch (WebClientResponseException e) {
			log.error("Error calling token-service to get status level: status={}, message={}",
					e.getStatusCode(), e.getMessage());
			throw new RuntimeException("Failed to get status level: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error getting status level", e);
			throw new RuntimeException("Failed to get status level: " + e.getMessage(), e);
		}
	}

	/**
	 * Create a new status level in token-service
	 *
	 * @param createRequest Map representing StatusLevelRequest
	 * @return Created status level as Map
	 */
	public Map<String, Object> createStatusLevel(Map<String, Object> createRequest) {
		try {
			log.info("Calling token-service to create status level: {}", createRequest.get("levelName"));
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/v1/status-levels")
					.bodyValue(createRequest)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.info("Status level created successfully in token-service: {}", response.getData().get("levelId"));
				return response.getData();
			} else {
				log.error("Failed to create status level in token-service. Response: {}", response);
				throw new RuntimeException("Failed to create status level.");
			}
		} catch (WebClientResponseException e) {
			log.error("Error creating status level in token-service: status={}, message={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to create status level: " + e.getResponseBodyAsString(), e);
		} catch (Exception e) {
			log.error("Unexpected error creating status level", e);
			throw new RuntimeException("Failed to create status level: " + e.getMessage(), e);
		}
	}

	/**
	 * Update an existing status level in token-service
	 *
	 * @param levelName     The level name of the status level to update
	 * @param updateRequest Map representing StatusLevelRequest
	 * @return Updated status level as Map
	 */
	public Map<String, Object> updateStatusLevel(String levelName, Map<String, Object> updateRequest) {
		try {
			log.info("Calling token-service to update status level: {}", levelName);
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/v1/status-levels/{level}", levelName)
					.bodyValue(updateRequest)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.info("Status level updated successfully in token-service: {}", response.getData().get("levelId"));
				return response.getData();
			} else {
				log.error("Failed to update status level in token-service. Response: {}", response);
				throw new RuntimeException("Failed to update status level.");
			}
		} catch (WebClientResponseException e) {
			log.error("Error updating status level in token-service: status={}, message={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to update status level: " + e.getResponseBodyAsString(), e);
		} catch (Exception e) {
			log.error("Unexpected error updating status level", e);
			throw new RuntimeException("Failed to update status level: " + e.getMessage(), e);
		}
	}

	/**
	 * Delete a status level in token-service
	 *
	 * @param levelName The level name of the status level to delete
	 */
	public void deleteStatusLevel(String levelName) {
		try {
			log.info("Calling token-service to delete status level: {}", levelName);
			getWebClient()
					.delete()
					.uri("/api/v1/status-levels/{level}", levelName)
					.retrieve()
					.bodyToMono(Void.class)
					.block();
			log.info("Status level deleted successfully in token-service: {}", levelName);
		} catch (WebClientResponseException.NotFound e) {
			log.error("Status level not found in token-service: levelName={}", levelName);
			throw new RuntimeException("Status level not found: " + levelName, e);
		} catch (WebClientResponseException e) {
			log.error("Error calling token-service to delete status level: status={}, message={}",
					e.getStatusCode(), e.getResponseBodyAsString());
			throw new RuntimeException("Failed to delete status level: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error deleting status level", e);
			throw new RuntimeException("Failed to delete status level: " + e.getMessage(), e);
		}
	}

	/**
	 * Get user token balance from token-service admin endpoint
	 *
	 * @param userId The user ID to get balance for
	 * @return Balance information as Map (to avoid DTO class conflicts)
	 */
	public Map<String, Object> getUserTokenBalance(Long userId) {
		try {
			log.debug("Calling token-service admin endpoint to get user token balance: userId={}", userId);
			String fullUrl = tokenServiceUrl + "/api/v1/admin/tokens/users/" + userId + "/balance";
			log.info("Calling token-service admin endpoint: {}", fullUrl);

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/v1/admin/tokens/users/{userId}/balance", userId)
					.retrieve()
					.onStatus(httpStatus -> httpStatus.isError(), clientResponse -> {
						// Don't block here - just log the status and create exception
						log.error("Error response from token-service when getting user balance: status={}",
								clientResponse.statusCode());
						return clientResponse.createException();
					})
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block(java.time.Duration.ofSeconds(10));

			if (response != null && response.getData() != null) {
				log.debug("Successfully retrieved token balance from token-service: userId={}", userId);
				return response.getData();
			} else {
				log.warn("Response from token-service was null or had no data for userId: {}", userId);
				throw new RuntimeException("Failed to get token balance: response was null or empty");
			}
		} catch (WebClientResponseException e) {
			log.error("WebClient error calling token-service to get user balance: status={}, body={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to get token balance from token-service: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error calling token-service to get user balance: userId={}", userId, e);
			throw new RuntimeException("Failed to get token balance: " + e.getMessage(), e);
		}
	}

	/**
	 * Get user transactions from token-service admin endpoint
	 *
	 * @param userId   The user ID to get transactions for
	 * @param type     Optional transaction type filter (e.g., "DEPOSIT", "WITHDRAWAL")
	 * @param status   Optional transaction status filter (e.g., "PENDING", "CONFIRMED")
	 * @param pageable Pagination parameters
	 * @return Page data as Map (to avoid DTO class conflicts)
	 */
	public Map<String, Object> getUserTransactions(
			Long userId,
			String type,
			String status,
			org.springframework.data.domain.Pageable pageable) {
		try {
			log.debug("Calling token-service admin endpoint to get user transactions: userId={}, type={}, status={}",
					userId, type, status);

			StringBuilder uriBuilder = new StringBuilder("/api/v1/admin/tokens/users/")
					.append(userId)
					.append("/transactions");

			boolean hasParams = false;
			if (pageable != null) {
				uriBuilder.append("?page=").append(pageable.getPageNumber())
						.append("&size=").append(pageable.getPageSize());
				hasParams = true;
			} else {
				uriBuilder.append("?page=0&size=50");
				hasParams = true;
			}

			if (type != null && !type.isEmpty()) {
				uriBuilder.append(hasParams ? "&" : "?")
						.append("type=").append(type);
				hasParams = true;
			}

			if (status != null && !status.isEmpty()) {
				uriBuilder.append(hasParams ? "&" : "?")
						.append("status=").append(status);
			}

			String fullUrl = tokenServiceUrl + uriBuilder.toString();
			log.info("Calling token-service admin endpoint: {}", fullUrl);

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder.toString())
					.retrieve()
					.onStatus(httpStatus -> httpStatus.isError(), clientResponse -> {
						log.error("Error response from token-service when getting user transactions: status={}",
								clientResponse.statusCode());
						return clientResponse.createException();
					})
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Successfully retrieved transactions from token-service: userId={}, totalElements={}",
						userId, response.getData().getOrDefault("totalElements", 0));
				return response.getData();
			} else {
				log.warn("Response from token-service was null or had no data for userId: {}", userId);
				Map<String, Object> emptyPage = new java.util.HashMap<>();
				emptyPage.put("content", new java.util.ArrayList<>());
				emptyPage.put("totalElements", 0L);
				emptyPage.put("totalPages", 0);
				emptyPage.put("number", 0);
				emptyPage.put("size", pageable != null ? pageable.getPageSize() : 50);
				return emptyPage;
			}
		} catch (WebClientResponseException e) {
			log.error("WebClient error calling token-service to get user transactions: status={}, body={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to get transactions from token-service: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error calling token-service to get user transactions: userId={}", userId, e);
			throw new RuntimeException("Failed to get transactions: " + e.getMessage(), e);
		}
	}

	/**
	 * Burn tokens for a user
	 */
	public Map<String, Object> burnTokens(Long userId, java.math.BigDecimal amount, String reason) {
		try {
			log.debug("Calling token-service to burn tokens: userId={}, amount={}, reason={}", userId, amount, reason);

			Map<String, Object> requestBody = new java.util.HashMap<>();
			requestBody.put("amount", amount);
			requestBody.put("reason", reason);

			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/v1/admin/tokens/users/{userId}/burn", userId)
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Successfully burned tokens for user: {}", userId);
				return response.getData();
			} else {
				log.warn("Response from token-service was null or had no data for burn operation: userId={}", userId);
				throw new RuntimeException("Failed to burn tokens: empty response");
			}
		} catch (WebClientResponseException e) {
			log.error("WebClient error calling token-service to burn tokens: status={}, body={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to burn tokens: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error calling token-service to burn tokens: userId={}", userId, e);
			throw new RuntimeException("Failed to burn tokens: " + e.getMessage(), e);
		}
	}

	/**
	 * Get user status
	 */
	public Map<String, Object> getUserStatus(Long userId) {
		try {
			log.debug("Calling token-service to get user status: userId={}", userId);

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/v1/admin/tokens/users/{userId}/status", userId)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Successfully retrieved user status from token-service: userId={}", userId);
				return response.getData();
			} else {
				log.warn("Response from token-service was null or had no data for user status: userId={}", userId);
				throw new RuntimeException("Failed to get user status: empty response");
			}
		} catch (WebClientResponseException e) {
			log.error("WebClient error calling token-service to get user status: status={}, body={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to get user status: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error calling token-service to get user status: userId={}", userId, e);
			throw new RuntimeException("Failed to get user status: " + e.getMessage(), e);
		}
	}

	/**
	 * Get deposit transactions with filters
	 */
	public Map<String, Object> getDepositTransactions(String status, String network,
	                                                  java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate,
	                                                  org.springframework.data.domain.Pageable pageable) {
		try {
			log.debug("Calling token-service to get deposit transactions: status={}, network={}", status, network);

			var uriBuilder = org.springframework.web.util.UriComponentsBuilder
					.fromPath("/api/v1/admin/tokens/transactions/deposits");

			if (status != null) uriBuilder.queryParam("status", status);
			if (network != null) uriBuilder.queryParam("network", network);
			if (startDate != null) uriBuilder.queryParam("startDate", startDate);
			if (endDate != null) uriBuilder.queryParam("endDate", endDate);
			if (pageable != null) {
				uriBuilder.queryParam("page", pageable.getPageNumber());
				uriBuilder.queryParam("size", pageable.getPageSize());
			}

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder.toUriString())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Successfully retrieved deposit transactions from token-service");
				return response.getData();
			} else {
				log.warn("Response from token-service was null or had no data for deposit transactions");
				Map<String, Object> emptyPage = new java.util.HashMap<>();
				emptyPage.put("content", new java.util.ArrayList<>());
				emptyPage.put("totalElements", 0L);
				return emptyPage;
			}
		} catch (WebClientResponseException e) {
			log.error("WebClient error calling token-service to get deposit transactions: status={}, body={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to get deposit transactions: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error calling token-service to get deposit transactions", e);
			throw new RuntimeException("Failed to get deposit transactions: " + e.getMessage(), e);
		}
	}

	/**
	 * Get withdrawal transactions with filters
	 */
	public Map<String, Object> getWithdrawalTransactions(String status, String network,
	                                                     java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate,
	                                                     org.springframework.data.domain.Pageable pageable) {
		try {
			log.debug("Calling token-service to get withdrawal transactions: status={}, network={}", status, network);

			var uriBuilder = org.springframework.web.util.UriComponentsBuilder
					.fromPath("/api/v1/admin/tokens/transactions/withdrawals");

			if (status != null) uriBuilder.queryParam("status", status);
			if (network != null) uriBuilder.queryParam("network", network);
			if (startDate != null) uriBuilder.queryParam("startDate", startDate);
			if (endDate != null) uriBuilder.queryParam("endDate", endDate);
			if (pageable != null) {
				uriBuilder.queryParam("page", pageable.getPageNumber());
				uriBuilder.queryParam("size", pageable.getPageSize());
			}

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder.toUriString())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Successfully retrieved withdrawal transactions from token-service");
				return response.getData();
			} else {
				log.warn("Response from token-service was null or had no data for withdrawal transactions");
				Map<String, Object> emptyPage = new java.util.HashMap<>();
				emptyPage.put("content", new java.util.ArrayList<>());
				emptyPage.put("totalElements", 0L);
				return emptyPage;
			}
		} catch (WebClientResponseException e) {
			log.error("WebClient error calling token-service to get withdrawal transactions: status={}, body={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to get withdrawal transactions: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error calling token-service to get withdrawal transactions", e);
			throw new RuntimeException("Failed to get withdrawal transactions: " + e.getMessage(), e);
		}
	}

	/**
	 * Get burn transactions with filters
	 */
	public Map<String, Object> getBurnTransactions(
			java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate,
			org.springframework.data.domain.Pageable pageable) {
		try {
			log.debug("Calling token-service to get burn transactions");

			var uriBuilder = org.springframework.web.util.UriComponentsBuilder
					.fromPath("/api/v1/admin/tokens/transactions/burns");

			if (startDate != null) uriBuilder.queryParam("startDate", startDate);
			if (endDate != null) uriBuilder.queryParam("endDate", endDate);
			if (pageable != null) {
				uriBuilder.queryParam("page", pageable.getPageNumber());
				uriBuilder.queryParam("size", pageable.getPageSize());
			}

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder.toUriString())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Successfully retrieved burn transactions from token-service");
				return response.getData();
			} else {
				log.warn("Response from token-service was null or had no data for burn transactions");
				Map<String, Object> emptyPage = new java.util.HashMap<>();
				emptyPage.put("content", new java.util.ArrayList<>());
				emptyPage.put("totalElements", 0L);
				return emptyPage;
			}
		} catch (WebClientResponseException e) {
			log.error("WebClient error calling token-service to get burn transactions: status={}, body={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to get burn transactions: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error calling token-service to get burn transactions", e);
			throw new RuntimeException("Failed to get burn transactions: " + e.getMessage(), e);
		}
	}

	/**
	 * Get claim transactions (staking rewards) with filters
	 */
	public Map<String, Object> getClaimTransactions(Long userId,
	                                                java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate,
	                                                org.springframework.data.domain.Pageable pageable) {
		try {
			log.debug("Calling token-service to get claim transactions: userId={}", userId);

			var uriBuilder = org.springframework.web.util.UriComponentsBuilder
					.fromPath("/api/v1/admin/tokens/transactions/claims");

			if (userId != null) uriBuilder.queryParam("userId", userId);
			if (startDate != null) uriBuilder.queryParam("startDate", startDate);
			if (endDate != null) uriBuilder.queryParam("endDate", endDate);
			if (pageable != null) {
				uriBuilder.queryParam("page", pageable.getPageNumber());
				uriBuilder.queryParam("size", pageable.getPageSize());
			}

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder.toUriString())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Successfully retrieved claim transactions from token-service");
				return response.getData();
			} else {
				log.warn("Response from token-service was null or had no data for claim transactions");
				Map<String, Object> emptyPage = new java.util.HashMap<>();
				emptyPage.put("content", new java.util.ArrayList<>());
				emptyPage.put("totalElements", 0L);
				return emptyPage;
			}
		} catch (WebClientResponseException e) {
			log.error("WebClient error calling token-service to get claim transactions: status={}, body={}",
					e.getStatusCode(), e.getResponseBodyAsString(), e);
			throw new RuntimeException("Failed to get claim transactions: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error calling token-service to get claim transactions", e);
			throw new RuntimeException("Failed to get claim transactions: " + e.getMessage(), e);
		}
	}
}

