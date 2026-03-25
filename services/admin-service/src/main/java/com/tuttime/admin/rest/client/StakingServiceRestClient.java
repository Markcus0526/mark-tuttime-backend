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
 * REST Client for Staking Service
 *
 * <p>Used for admin operations that require REST API calls to staking-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StakingServiceRestClient {

	private final WebClient.Builder webClientBuilder;

	@Value("${services.staking-service.url:${STAKING_SERVICE_URL:http://localhost:28084}}")
	private String stakingServiceUrl;

	private WebClient getWebClient() {
		return webClientBuilder.clone()
				.baseUrl(stakingServiceUrl)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	/**
	 * Get all active staking pools from staking-service
	 *
	 * @return List of staking pools as Map (to avoid DTO class conflicts)
	 */
	public List<Map<String, Object>> getActivePools() {
		try {
			log.debug("Calling staking-service to get active pools: {}", stakingServiceUrl);
			ApiResponse<List<Map<String, Object>>> response = getWebClient()
					.get()
					.uri("/api/v1/staking/pools")
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
					})
					.timeout(java.time.Duration.ofSeconds(5))
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Retrieved {} staking pools from staking-service", response.getData().size());
				return response.getData();
			} else {
				log.warn("No pools returned from staking-service");
				return List.of();
			}
		} catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
			// Connection refused or service unavailable - log as warning, not error
			if (e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
					e.getMessage().contains("Connection timed out") ||
					e.getMessage().contains("Service Unavailable"))) {
				log.warn("Staking-service is not available (connection refused/timeout). Returning empty pools list. URL: {}", stakingServiceUrl);
				return List.of();
			}
			log.error("Network error calling staking-service to get pools: {}", e.getMessage());
			throw new RuntimeException("Failed to get staking pools: " + e.getMessage(), e);
		} catch (WebClientResponseException e) {
			log.error("Error calling staking-service to get pools: status={}, message={}",
					e.getStatusCode(), e.getMessage());
			throw new RuntimeException("Failed to get staking pools: " + e.getMessage(), e);
		} catch (Exception e) {
			// Check if it's a connection error
			if (e.getCause() instanceof java.net.ConnectException ||
					e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
							e.getMessage().contains("Connection timed out"))) {
				log.warn("Staking-service is not available. Returning empty pools list. URL: {}", stakingServiceUrl);
				return List.of();
			}
			log.error("Unexpected error getting staking pools", e);
			throw new RuntimeException("Failed to get staking pools: " + e.getMessage(), e);
		}
	}

	/**
	 * Get a staking pool by ID from staking-service
	 *
	 * @param poolId The pool ID
	 * @return Staking pool as Map (to avoid DTO class conflicts)
	 */
	public Map<String, Object> getPoolById(Long poolId) {
		try {
			log.debug("Calling staking-service to get pool by ID: {} from {}", poolId, stakingServiceUrl);
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/v1/staking/pools/{poolId}", poolId)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Retrieved pool {} from staking-service", poolId);
				return response.getData();
			} else {
				log.warn("No pool returned from staking-service for poolId: {}", poolId);
				throw new RuntimeException("Pool not found: " + poolId);
			}
		} catch (WebClientResponseException.NotFound e) {
			log.error("Pool not found in staking-service: poolId={}", poolId);
			throw new RuntimeException("Pool not found: " + poolId, e);
		} catch (WebClientResponseException e) {
			log.error("Error calling staking-service to get pool: status={}, message={}",
					e.getStatusCode(), e.getMessage());
			throw new RuntimeException("Failed to get staking pool: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error getting staking pool", e);
			throw new RuntimeException("Failed to get staking pool: " + e.getMessage(), e);
		}
	}

	/**
	 * Create a new staking pool in staking-service
	 *
	 * @param requestBody The create pool request as Map
	 * @return Created staking pool as Map
	 */
	public Map<String, Object> createPool(Map<String, Object> requestBody) {
		try {
			log.debug("Calling staking-service to create pool: {}", stakingServiceUrl);
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/v1/staking/pools")
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Created pool in staking-service: poolId={}", response.getData().get("poolId"));
				return response.getData();
			} else {
				log.warn("No pool returned from staking-service after creation");
				throw new RuntimeException("Failed to create staking pool: no response data");
			}
		} catch (WebClientResponseException e) {
			log.error("Error calling staking-service to create pool: status={}, message={}",
					e.getStatusCode(), e.getResponseBodyAsString());
			throw new RuntimeException("Failed to create staking pool: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error creating staking pool", e);
			throw new RuntimeException("Failed to create staking pool: " + e.getMessage(), e);
		}
	}

	/**
	 * Update an existing staking pool in staking-service
	 *
	 * @param poolId      The pool ID to update
	 * @param requestBody The update pool request as Map
	 * @return Updated staking pool as Map
	 */
	public Map<String, Object> updatePool(Long poolId, Map<String, Object> requestBody) {
		try {
			log.debug("Calling staking-service to update pool: poolId={}, url={}", poolId, stakingServiceUrl);
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/v1/staking/pools/{poolId}", poolId)
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				log.debug("Updated pool in staking-service: poolId={}", poolId);
				return response.getData();
			} else {
				log.warn("No pool returned from staking-service after update");
				throw new RuntimeException("Failed to update staking pool: no response data");
			}
		} catch (WebClientResponseException.NotFound e) {
			log.error("Pool not found in staking-service: poolId={}", poolId);
			throw new RuntimeException("Pool not found: " + poolId, e);
		} catch (WebClientResponseException e) {
			log.error("Error calling staking-service to update pool: status={}, message={}",
					e.getStatusCode(), e.getResponseBodyAsString());
			throw new RuntimeException("Failed to update staking pool: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error updating staking pool", e);
			throw new RuntimeException("Failed to update staking pool: " + e.getMessage(), e);
		}
	}

	/**
	 * Delete a staking pool in staking-service
	 *
	 * @param poolId The pool ID to delete
	 */
	public void deletePool(Long poolId) {
		try {
			log.debug("Calling staking-service to delete pool: poolId={}, url={}", poolId, stakingServiceUrl);
			getWebClient()
					.delete()
					.uri("/api/v1/staking/pools/{poolId}", poolId)
					.retrieve()
					.bodyToMono(Void.class)
					.block();
			log.debug("Deleted pool in staking-service: poolId={}", poolId);
		} catch (WebClientResponseException.NotFound e) {
			log.error("Pool not found in staking-service: poolId={}", poolId);
			throw new RuntimeException("Pool not found: " + poolId, e);
		} catch (WebClientResponseException e) {
			log.error("Error calling staking-service to delete pool: status={}, message={}",
					e.getStatusCode(), e.getResponseBodyAsString());
			throw new RuntimeException("Failed to delete staking pool: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error deleting staking pool", e);
			throw new RuntimeException("Failed to delete staking pool: " + e.getMessage(), e);
		}
	}
}

