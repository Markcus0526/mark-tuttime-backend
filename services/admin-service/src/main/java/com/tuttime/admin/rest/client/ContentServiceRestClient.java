package com.tuttime.admin.rest.client;

import com.tuttime.admin.dto.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Client for Content Service
 *
 * <p>Used for admin operations to manage tasks and ads via content-service REST API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentServiceRestClient {

	private final WebClient.Builder webClientBuilder;

	@Value("${services.content-service.url:http://localhost:28086}")
	private String contentServiceUrl;

	private WebClient getWebClient() {
		return webClientBuilder.clone()
				.baseUrl(contentServiceUrl)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	// ==================== Task Methods ====================

	public Page<TaskResponse> listTasks(Pageable pageable, String status) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder -> {
						org.springframework.web.util.UriBuilder builder = uriBuilder
								.path("/api/admin/tasks")
								.queryParam("page", pageable.getPageNumber())
								.queryParam("size", pageable.getPageSize());
						if (status != null && !status.trim().isEmpty()) {
							builder.queryParam("status", status);
						}
						return builder.build();
					})
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				Map<String, Object> pageData = response.getData();
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.getOrDefault("content", new java.util.ArrayList<>());
				long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
				int number = ((Number) pageData.getOrDefault("number", pageable.getPageNumber())).intValue();
				int size = ((Number) pageData.getOrDefault("size", pageable.getPageSize())).intValue();

				return new PageImpl<>(
						content.stream()
								.map(this::mapToTaskResponse)
								.toList(),
						PageRequest.of(number, size),
						totalElements
				);
			}
			return new PageImpl<>(java.util.Collections.emptyList());
		} catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
			// Connection refused or service unavailable - return empty page
			if (e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
					e.getMessage().contains("Connection timed out") ||
					e.getMessage().contains("Service Unavailable"))) {
				log.warn("Content-service is not available (connection refused/timeout). Returning empty tasks list. URL: {}", contentServiceUrl);
				return new PageImpl<>(java.util.Collections.emptyList());
			}
			log.error("Network error listing tasks", e);
			throw new RuntimeException("Failed to list tasks: " + e.getMessage(), e);
		} catch (WebClientResponseException e) {
			log.error("Error listing tasks", e);
			throw new RuntimeException("Failed to list tasks: " + e.getMessage(), e);
		} catch (Exception e) {
			// Check if it's a connection error
			if (e.getCause() instanceof java.net.ConnectException ||
					e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
							e.getMessage().contains("Connection timed out"))) {
				log.warn("Content-service is not available. Returning empty tasks list. URL: {}", contentServiceUrl);
				return new PageImpl<>(java.util.Collections.emptyList());
			}
			log.error("Unexpected error listing tasks", e);
			throw new RuntimeException("Failed to list tasks: " + e.getMessage(), e);
		}
	}

	public TaskResponse getTask(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/tasks/{id}", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToTaskResponse(response.getData());
			}
			throw new RuntimeException("Task not found: " + id);
		} catch (WebClientResponseException.NotFound e) {
			throw new RuntimeException("Task not found: " + id, e);
		} catch (WebClientResponseException e) {
			log.error("Error getting task: {}", id, e);
			throw new RuntimeException("Failed to get task: " + e.getMessage(), e);
		}
	}

	public TaskResponse createTask(TaskRequest request, Long adminId) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/tasks")
					.header("X-Admin-Id", adminId != null ? adminId.toString() : "")
					.bodyValue(convertToTaskRequest(request))
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToTaskResponse(response.getData());
			}
			throw new RuntimeException("Failed to create task");
		} catch (WebClientResponseException e) {
			log.error("Error creating task", e);
			throw new RuntimeException("Failed to create task: " + e.getMessage(), e);
		}
	}

	public TaskResponse updateTask(Long id, TaskRequest request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/admin/tasks/{id}", id)
					.bodyValue(convertToTaskRequest(request))
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToTaskResponse(response.getData());
			}
			throw new RuntimeException("Failed to update task");
		} catch (WebClientResponseException e) {
			log.error("Error updating task: {}", id, e);
			throw new RuntimeException("Failed to update task: " + e.getMessage(), e);
		}
	}

	public void deleteTask(Long id) {
		try {
			getWebClient()
					.delete()
					.uri("/api/admin/tasks/{id}", id)
					.retrieve()
					.bodyToMono(Void.class)
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();
		} catch (WebClientResponseException e) {
			log.error("Error deleting task: {}", id, e);
			throw new RuntimeException("Failed to delete task: " + e.getMessage(), e);
		}
	}

	public TaskResponse activateTask(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/tasks/{id}/activate", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToTaskResponse(response.getData());
			}
			throw new RuntimeException("Failed to activate task");
		} catch (WebClientResponseException e) {
			log.error("Error activating task: {}", id, e);
			throw new RuntimeException("Failed to activate task: " + e.getMessage(), e);
		}
	}

	public TaskResponse deactivateTask(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/tasks/{id}/deactivate", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToTaskResponse(response.getData());
			}
			throw new RuntimeException("Failed to deactivate task");
		} catch (WebClientResponseException e) {
			log.error("Error deactivating task: {}", id, e);
			throw new RuntimeException("Failed to deactivate task: " + e.getMessage(), e);
		}
	}

	// ==================== Watch Video Task Statistics Methods ====================

	public Map<String, Object> getDailyWatchVideoStatistics(java.time.OffsetDateTime date) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder -> {
						org.springframework.web.util.UriBuilder builder = uriBuilder
								.path("/api/admin/tasks/watch-video/statistics/daily");
						if (date != null) {
							builder.queryParam("date", date.toString());
						}
						return builder.build();
					})
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to get daily watch video statistics");
		} catch (WebClientResponseException e) {
			log.error("Error getting daily watch video statistics", e);
			throw new RuntimeException("Failed to get daily watch video statistics: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> getWatchVideoTaskStatistics(Long taskId) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/tasks/watch-video/statistics/task/{taskId}", taskId)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to get watch video task statistics");
		} catch (WebClientResponseException e) {
			log.error("Error getting watch video task statistics for task: {}", taskId, e);
			throw new RuntimeException("Failed to get watch video task statistics: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> getUserWatchVideoStatistics(Long userId) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/tasks/watch-video/statistics/user/{userId}", userId)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to get user watch video statistics");
		} catch (WebClientResponseException e) {
			log.error("Error getting user watch video statistics for user: {}", userId, e);
			throw new RuntimeException("Failed to get user watch video statistics: " + e.getMessage(), e);
		}
	}

	public List<Map<String, Object>> getDateRangeWatchVideoStatistics(
			java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate) {
		try {
			ApiResponse<List<Map<String, Object>>> response = getWebClient()
					.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/admin/tasks/watch-video/statistics/range")
							.queryParam("startDate", startDate.toString())
							.queryParam("endDate", endDate.toString())
							.build())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to get date range watch video statistics");
		} catch (WebClientResponseException e) {
			log.error("Error getting date range watch video statistics", e);
			throw new RuntimeException("Failed to get date range watch video statistics: " + e.getMessage(), e);
		}
	}

	/**
	 * Get popular tasks completed by users per day
	 */
	public List<Map<String, Object>> getPopularTasksPerDay(
			java.time.LocalDate startDate,
			java.time.LocalDate endDate) {
		try {
			ApiResponse<List<Map<String, Object>>> response = getWebClient()
					.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/admin/analytics/tasks/popular/daily")
							.queryParam("startDate", startDate != null ? startDate.toString() : "")
							.queryParam("endDate", endDate != null ? endDate.toString() : "")
							.build())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			return new java.util.ArrayList<>();
		} catch (WebClientResponseException.ServiceUnavailable |
		         org.springframework.web.reactive.function.client.WebClientRequestException e) {
			log.warn("Content-service is not available. Returning empty popular tasks per day list. URL: {}", contentServiceUrl);
			return new java.util.ArrayList<>();
		} catch (WebClientResponseException e) {
			log.error("Error getting popular tasks per day", e);
			throw new RuntimeException("Failed to get popular tasks per day: " + e.getMessage(), e);
		}
	}

	/**
	 * Get popular tasks completed by users per month
	 */
	public List<Map<String, Object>> getPopularTasksPerMonth(
			java.time.LocalDate startDate,
			java.time.LocalDate endDate) {
		try {
			ApiResponse<List<Map<String, Object>>> response = getWebClient()
					.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/admin/analytics/tasks/popular/monthly")
							.queryParam("startDate", startDate != null ? startDate.toString() : "")
							.queryParam("endDate", endDate != null ? endDate.toString() : "")
							.build())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			return new java.util.ArrayList<>();
		} catch (WebClientResponseException.ServiceUnavailable |
		         org.springframework.web.reactive.function.client.WebClientRequestException e) {
			log.warn("Content-service is not available. Returning empty popular tasks per month list. URL: {}", contentServiceUrl);
			return new java.util.ArrayList<>();
		} catch (WebClientResponseException e) {
			log.error("Error getting popular tasks per month", e);
			throw new RuntimeException("Failed to get popular tasks per month: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> backfillExistingUsersWatchVideoTasks() {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/tasks/watch-video/backfill-existing-users")
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to backfill existing users");
		} catch (WebClientResponseException e) {
			log.error("Error backfilling existing users", e);
			throw new RuntimeException("Failed to backfill existing users: " + e.getMessage(), e);
		}
	}

	// ==================== Ad Methods ====================

	public Page<AdResponse> listAds(Pageable pageable, String status) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder -> {
						org.springframework.web.util.UriBuilder builder = uriBuilder
								.path("/api/admin/ads")
								.queryParam("page", pageable.getPageNumber())
								.queryParam("size", pageable.getPageSize());
						if (status != null && !status.trim().isEmpty()) {
							builder.queryParam("status", status);
						}
						return builder.build();
					})
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				Map<String, Object> pageData = response.getData();
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.getOrDefault("content", new java.util.ArrayList<>());
				long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
				int number = ((Number) pageData.getOrDefault("number", pageable.getPageNumber())).intValue();
				int size = ((Number) pageData.getOrDefault("size", pageable.getPageSize())).intValue();

				return new PageImpl<>(
						content.stream()
								.map(this::mapToAdResponse)
								.toList(),
						PageRequest.of(number, size),
						totalElements
				);
			}
			return new PageImpl<>(java.util.Collections.emptyList());
		} catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
			// Connection refused or service unavailable - return empty page
			if (e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
					e.getMessage().contains("Connection timed out") ||
					e.getMessage().contains("Service Unavailable"))) {
				log.warn("Content-service is not available (connection refused/timeout). Returning empty ads list. URL: {}", contentServiceUrl);
				return new PageImpl<>(java.util.Collections.emptyList());
			}
			log.error("Network error listing ads", e);
			throw new RuntimeException("Failed to list ads: " + e.getMessage(), e);
		} catch (WebClientResponseException e) {
			log.error("Error listing ads", e);
			throw new RuntimeException("Failed to list ads: " + e.getMessage(), e);
		} catch (Exception e) {
			// Check if it's a connection error
			if (e.getCause() instanceof java.net.ConnectException ||
					e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
							e.getMessage().contains("Connection timed out"))) {
				log.warn("Content-service is not available. Returning empty ads list. URL: {}", contentServiceUrl);
				return new PageImpl<>(java.util.Collections.emptyList());
			}
			log.error("Unexpected error listing ads", e);
			throw new RuntimeException("Failed to list ads: " + e.getMessage(), e);
		}
	}

	public AdResponse getAd(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/ads/{id}", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToAdResponse(response.getData());
			}
			throw new RuntimeException("Ad not found: " + id);
		} catch (WebClientResponseException.NotFound e) {
			throw new RuntimeException("Ad not found: " + id, e);
		} catch (WebClientResponseException e) {
			log.error("Error getting ad: {}", id, e);
			throw new RuntimeException("Failed to get ad: " + e.getMessage(), e);
		}
	}

	public AdResponse createAd(AdRequest request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/ads")
					.bodyValue(convertToAdRequest(request))
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToAdResponse(response.getData());
			}
			throw new RuntimeException("Failed to create ad");
		} catch (WebClientResponseException e) {
			log.error("Error creating ad", e);
			throw new RuntimeException("Failed to create ad: " + e.getMessage(), e);
		}
	}

	public AdResponse updateAd(Long id, AdRequest request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/admin/ads/{id}", id)
					.bodyValue(convertToAdRequest(request))
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToAdResponse(response.getData());
			}
			throw new RuntimeException("Failed to update ad");
		} catch (WebClientResponseException e) {
			log.error("Error updating ad: {}", id, e);
			throw new RuntimeException("Failed to update ad: " + e.getMessage(), e);
		}
	}

	public void deleteAd(Long id) {
		try {
			getWebClient()
					.delete()
					.uri("/api/admin/ads/{id}", id)
					.retrieve()
					.bodyToMono(Void.class)
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();
		} catch (WebClientResponseException e) {
			log.error("Error deleting ad: {}", id, e);
			throw new RuntimeException("Failed to delete ad: " + e.getMessage(), e);
		}
	}

	public AdResponse approveAd(Long id, Long adminId) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/ads/{id}/approve", id)
					.header("X-Admin-Id", adminId != null ? adminId.toString() : "")
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return mapToAdResponse(response.getData());
			}
			throw new RuntimeException("Failed to approve ad");
		} catch (WebClientResponseException e) {
			String responseBody = e.getResponseBodyAsString();
			log.error("Error approving ad {}: Status={}, Body={}", id, e.getStatusCode(), responseBody, e);
			throw new RuntimeException("Failed to approve ad: " + e.getStatusCode() + " - " + responseBody, e);
		} catch (Exception e) {
			log.error("Unexpected error approving ad: {}", id, e);
			throw new RuntimeException("Failed to approve ad: " + e.getMessage(), e);
		}
	}

	public AdResponse rejectAd(Long id, Long adminId, com.tuttime.admin.dto.RejectRequest request) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("reason", request.getReason());

			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/ads/{id}/reject", id)
					.header("X-Admin-Id", adminId != null ? adminId.toString() : "")
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToAdResponse(response.getData());
			}
			throw new RuntimeException("Failed to reject ad");
		} catch (WebClientResponseException e) {
			log.error("Error rejecting ad: {}", id, e);
			throw new RuntimeException("Failed to reject ad: " + e.getMessage(), e);
		}
	}

	// ==================== Helper Methods ====================

	private Map<String, Object> convertToTaskRequest(TaskRequest request) {
		Map<String, Object> map = new HashMap<>();
		map.put("taskTitle", request.getTaskTitle() != null ? request.getTaskTitle() : "");
		map.put("taskDescription", request.getTaskDescription() != null ? request.getTaskDescription() : "");
		map.put("taskType", request.getTaskType() != null ? request.getTaskType() : "");
		map.put("bonusAp", request.getBonusAp() != null ? request.getBonusAp() : java.math.BigDecimal.ZERO);
		map.put("status", request.getStatus() != null ? request.getStatus() : "INACTIVE");
		map.put("maxCompletions", request.getMaxCompletions());
		map.put("startDate", request.getStartDate());
		map.put("endDate", request.getEndDate());
		map.put("isRepeatable", request.getIsRepeatable() != null ? request.getIsRepeatable() : false);
		map.put("repeatIntervalDays", request.getRepeatIntervalDays());
		map.put("actionType", request.getActionType());
		map.put("metadata", request.getMetadata() != null ? request.getMetadata() : java.util.Collections.emptyMap());
		return map;
	}

	private Map<String, Object> convertToAdRequest(AdRequest request) {
		Map<String, Object> map = new HashMap<>();
		map.put("advertiserUserId", request.getAdvertiserUserId());
		map.put("adTitle", request.getAdTitle() != null ? request.getAdTitle() : "");
		map.put("adDescription", request.getAdDescription() != null ? request.getAdDescription() : "");
		map.put("adType", request.getAdType() != null ? request.getAdType() : "");
		map.put("contentUrl", request.getContentUrl() != null ? request.getContentUrl() : "");
		map.put("externalLink", request.getExternalLink() != null ? request.getExternalLink() : "");
		map.put("targetUrl", request.getTargetUrl() != null ? request.getTargetUrl() : "");
		map.put("paymentAmount", request.getPaymentAmount() != null ? request.getPaymentAmount() : java.math.BigDecimal.ZERO);
		map.put("burnPercentage", request.getBurnPercentage());
		map.put("status", request.getStatus() != null ? request.getStatus() : "DRAFT");
		map.put("startDate", request.getStartDate());
		map.put("endDate", request.getEndDate());
		map.put("isHideable", request.getIsHideable() != null ? request.getIsHideable() : false);
		map.put("boostPerView", request.getBoostPerView() != null ? request.getBoostPerView() : java.math.BigDecimal.ZERO);
		return map;
	}

	private TaskResponse mapToTaskResponse(Map<String, Object> data) {
		// Extract actionType from metadata if not directly present
		String actionType = (String) data.get("actionType");
		Map<String, Object> metadata = getMetadataMap(data);
		if (actionType == null && metadata != null && metadata.containsKey("actionType")) {
			actionType = String.valueOf(metadata.get("actionType"));
		}

		return TaskResponse.builder()
				.taskId(getLongValue(data, "taskId"))
				.taskTitle((String) data.get("taskTitle"))
				.taskDescription((String) data.get("taskDescription"))
				.taskType((String) data.get("taskType"))
				.bonusAp(getBigDecimalValue(data, "bonusAp"))
				.status((String) data.get("status"))
				.maxCompletions(getIntegerValue(data, "maxCompletions"))
				.currentCompletions(getIntegerValue(data, "currentCompletions"))
				.startDate(parseDateTime(data.get("startDate")))
				.endDate(parseDateTime(data.get("endDate")))
				.isRepeatable(getBooleanValue(data, "isRepeatable"))
				.repeatIntervalDays(getIntegerValue(data, "repeatIntervalDays"))
				.actionType(actionType)
				.metadata(metadata)
				.createdAt(parseDateTime(data.get("createdAt")))
				.updatedAt(parseDateTime(data.get("updatedAt")))
				.createdBy(getLongValue(data, "createdBy"))
				.thumbnailUrl((String) data.get("thumbnailUrl"))
				.build();
	}

	private AdResponse mapToAdResponse(Map<String, Object> data) {
		return AdResponse.builder()
				.adId(getLongValue(data, "adId"))
				.advertiserUserId(getLongValue(data, "advertiserUserId"))
				.adTitle((String) data.get("adTitle"))
				.adDescription((String) data.get("adDescription"))
				.adType((String) data.get("adType"))
				.contentUrl((String) data.get("contentUrl"))
				.externalLink((String) data.get("externalLink"))
				.targetUrl((String) data.get("targetUrl"))
				.paymentAmount(getBigDecimalValue(data, "paymentAmount"))
				.burnPercentage(getBigDecimalValue(data, "burnPercentage"))
				.burnedAmount(getBigDecimalValue(data, "burnedAmount"))
				.totalViews(getIntegerValue(data, "totalViews"))
				.totalClicks(getIntegerValue(data, "totalClicks"))
				.status((String) data.get("status"))
				.startDate(parseDateTime(data.get("startDate")))
				.endDate(parseDateTime(data.get("endDate")))
				.isHideable(getBooleanValue(data, "isHideable"))
				.boostPerView(getBigDecimalValue(data, "boostPerView"))
				.approvedAt(parseDateTime(data.get("approvedAt")))
				.approvedBy(getLongValue(data, "approvedBy"))
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

	private Integer getIntegerValue(Map<String, Object> data, String key) {
		Object value = data.get(key);
		if (value == null) return 0;
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return Integer.parseInt(value.toString());
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

	// ==================== Video Methods ====================

	public Page<VideoResponse> listVideos(Pageable pageable, String status) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder -> {
						org.springframework.web.util.UriBuilder builder = uriBuilder
								.path("/api/admin/videos")
								.queryParam("page", pageable.getPageNumber())
								.queryParam("size", pageable.getPageSize());
						if (status != null && !status.trim().isEmpty()) {
							builder.queryParam("status", status);
						}
						return builder.build();
					})
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				Map<String, Object> pageData = response.getData();
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.getOrDefault("content", new java.util.ArrayList<>());
				long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
				int number = ((Number) pageData.getOrDefault("number", pageable.getPageNumber())).intValue();
				int size = ((Number) pageData.getOrDefault("size", pageable.getPageSize())).intValue();

				return new PageImpl<>(
						content.stream()
								.map(this::mapToVideoResponse)
								.toList(),
						PageRequest.of(number, size),
						totalElements
				);
			}
			return new PageImpl<>(java.util.Collections.emptyList());
		} catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
			// Connection refused or service unavailable - return empty page
			if (e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
					e.getMessage().contains("Connection timed out") ||
					e.getMessage().contains("Service Unavailable"))) {
				log.warn("Content-service is not available (connection refused/timeout). Returning empty videos list. URL: {}", contentServiceUrl);
				return new PageImpl<>(java.util.Collections.emptyList());
			}
			log.error("Network error listing videos", e);
			throw new RuntimeException("Failed to list videos: " + e.getMessage(), e);
		} catch (WebClientResponseException e) {
			log.error("Error listing videos", e);
			throw new RuntimeException("Failed to list videos: " + e.getMessage(), e);
		} catch (Exception e) {
			// Check if it's a connection error
			if (e.getCause() instanceof java.net.ConnectException ||
					e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
							e.getMessage().contains("Connection timed out"))) {
				log.warn("Content-service is not available. Returning empty videos list. URL: {}", contentServiceUrl);
				return new PageImpl<>(java.util.Collections.emptyList());
			}
			log.error("Unexpected error listing videos", e);
			throw new RuntimeException("Failed to list videos: " + e.getMessage(), e);
		}
	}

	public VideoResponse getVideo(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/videos/{id}", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToVideoResponse(response.getData());
			}
			throw new RuntimeException("Video not found: " + id);
		} catch (WebClientResponseException.NotFound e) {
			throw new RuntimeException("Video not found: " + id, e);
		} catch (WebClientResponseException e) {
			log.error("Error getting video: {}", id, e);
			throw new RuntimeException("Failed to get video: " + e.getMessage(), e);
		}
	}

	public VideoResponse createVideo(VideoRequest request, Long adminId) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/videos")
					.header("X-Admin-Id", adminId != null ? adminId.toString() : "")
					.bodyValue(convertToVideoRequest(request))
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToVideoResponse(response.getData());
			}
			throw new RuntimeException("Failed to create video");
		} catch (WebClientResponseException e) {
			log.error("Error creating video", e);
			throw new RuntimeException("Failed to create video: " + e.getMessage(), e);
		}
	}

	public VideoResponse updateVideo(Long id, VideoRequest request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/admin/videos/{id}", id)
					.bodyValue(convertToVideoRequest(request))
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToVideoResponse(response.getData());
			}
			throw new RuntimeException("Failed to update video");
		} catch (WebClientResponseException e) {
			log.error("Error updating video: {}", id, e);
			throw new RuntimeException("Failed to update video: " + e.getMessage(), e);
		}
	}

	public void deleteVideo(Long id) {
		try {
			getWebClient()
					.delete()
					.uri("/api/admin/videos/{id}", id)
					.retrieve()
					.bodyToMono(Void.class)
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();
		} catch (WebClientResponseException e) {
			log.error("Error deleting video: {}", id, e);
			throw new RuntimeException("Failed to delete video: " + e.getMessage(), e);
		}
	}

	public VideoResponse approveVideo(Long id, Long adminId) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/videos/{id}/approve", id)
					.header("X-Admin-Id", adminId != null ? adminId.toString() : "")
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToVideoResponse(response.getData());
			}
			throw new RuntimeException("Failed to approve video");
		} catch (WebClientResponseException e) {
			log.error("Error approving video: {}", id, e);
			throw new RuntimeException("Failed to approve video: " + e.getMessage(), e);
		}
	}

	public VideoResponse rejectVideo(Long id, Long adminId, com.tuttime.admin.dto.RejectRequest request) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("reason", request.getReason());

			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/videos/{id}/reject", id)
					.header("X-Admin-Id", adminId != null ? adminId.toString() : "")
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return mapToVideoResponse(response.getData());
			}
			throw new RuntimeException("Failed to reject video");
		} catch (WebClientResponseException e) {
			String responseBody = e.getResponseBodyAsString();
			log.error("Error rejecting video {}: Status={}, Body={}", id, e.getStatusCode(), responseBody, e);
			throw new RuntimeException("Failed to reject video: " + e.getStatusCode() + " - " + responseBody, e);
		} catch (Exception e) {
			log.error("Unexpected error rejecting video: {}", id, e);
			throw new RuntimeException("Failed to reject video: " + e.getMessage(), e);
		}
	}

	public VideoResponse publishVideo(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/videos/{id}/publish", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToVideoResponse(response.getData());
			}
			throw new RuntimeException("Failed to publish video");
		} catch (WebClientResponseException e) {
			log.error("Error publishing video: {}", id, e);
			throw new RuntimeException("Failed to publish video: " + e.getMessage(), e);
		}
	}

	public VideoResponse scheduleVideo(Long id, com.tuttime.admin.dto.ScheduleVideoRequest request) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("scheduledAt", request.getScheduledAt());

			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/videos/{id}/schedule", id)
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
					.block();

			if (response != null && response.getData() != null) {
				return mapToVideoResponse(response.getData());
			}
			throw new RuntimeException("Failed to schedule video");
		} catch (WebClientResponseException e) {
			log.error("Error scheduling video: {}", id, e);
			throw new RuntimeException("Failed to schedule video: " + e.getMessage(), e);
		}
	}

	// ==================== Helper Methods ====================

	private Map<String, Object> convertToVideoRequest(VideoRequest request) {
		Map<String, Object> map = new HashMap<>();
		map.put("advertiserUserId", request.getAdvertiserUserId());
		map.put("title", request.getTitle() != null ? request.getTitle() : "");
		map.put("description", request.getDescription() != null ? request.getDescription() : "");

		// If both s3FileKey and videoUrl are provided, send both
		// Content-service will prioritize videoUrl (the generated presigned URL) over s3FileKey
		String videoUrl = request.getVideoUrl();
		if (request.getS3FileKey() != null && !request.getS3FileKey().trim().isEmpty()) {
			// Pass s3FileKey separately (for reference/backup)
			map.put("s3FileKey", request.getS3FileKey());
			// Keep videoUrl if it's provided (presigned URL generated by admin-service)
			// If videoUrl is null, content-service will generate it from s3FileKey
		}
		map.put("videoUrl", videoUrl != null ? videoUrl : "");

		map.put("thumbnailUrl", request.getThumbnailUrl() != null ? request.getThumbnailUrl() : "");
		map.put("externalLink", request.getExternalLink() != null ? request.getExternalLink() : "");
		map.put("durationSeconds", request.getDurationSeconds() != null ? request.getDurationSeconds() : 0);
		map.put("status", request.getStatus() != null ? request.getStatus() : "PENDING");
		map.put("paymentAmount", request.getPaymentAmount() != null ? request.getPaymentAmount() : java.math.BigDecimal.ZERO);
		map.put("burnPercentage", request.getBurnPercentage());
		map.put("tokenCost", request.getTokenCost() != null ? request.getTokenCost() : java.math.BigDecimal.ZERO);
		map.put("boostAmount", request.getBoostAmount() != null ? request.getBoostAmount() : java.math.BigDecimal.ZERO);
		map.put("boostDurationHours", request.getBoostDurationHours() != null ? request.getBoostDurationHours() : 24);
		map.put("isActive", request.getIsActive() != null ? request.getIsActive() : true);
		map.put("scheduledPublishDate", request.getScheduledPublishDate());
		return map;
	}

	private VideoResponse mapToVideoResponse(Map<String, Object> data) {
		return VideoResponse.builder()
				.videoId(getLongValue(data, "videoId"))
				.advertiserUserId(getLongValue(data, "advertiserUserId"))
				.title((String) data.get("title"))
				.description((String) data.get("description"))
				.videoUrl((String) data.get("videoUrl"))
				.thumbnailUrl((String) data.get("thumbnailUrl"))
				.externalLink((String) data.get("externalLink"))
				.durationSeconds(getIntegerValue(data, "durationSeconds"))
				.status((String) data.get("status"))
				.totalViews(getIntegerValue(data, "totalViews"))
				.totalCompletedViews(getIntegerValue(data, "totalCompletedViews"))
				.paymentAmount(getBigDecimalValue(data, "paymentAmount"))
				.burnPercentage(getBigDecimalValue(data, "burnPercentage"))
				.burnedAmount(getBigDecimalValue(data, "burnedAmount"))
				.tokenCost(getBigDecimalValue(data, "tokenCost"))
				.boostAmount(getBigDecimalValue(data, "boostAmount"))
				.boostDurationHours(getIntegerValue(data, "boostDurationHours"))
				.isActive(getBooleanValue(data, "isActive"))
				.approvedAt(parseDateTime(data.get("approvedAt")))
				.approvedBy(getLongValue(data, "approvedBy"))
				.createdAt(parseDateTime(data.get("createdAt")))
				.updatedAt(parseDateTime(data.get("updatedAt")))
				.build();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getMetadataMap(Map<String, Object> data) {
		Object metadata = data.get("metadata");
		if (metadata == null) return null;
		return (Map<String, Object>) metadata;
	}

	/**
	 * Parses a datetime value to OffsetDateTime.
	 *
	 * <p><b>Timezone Note:</b> Parsed dates are assumed to be in UTC timezone.
	 * All API responses serialize dates in UTC, so this is safe.
	 *
	 * @param value The datetime value to parse (String or null)
	 * @return Parsed OffsetDateTime in UTC, or null if parsing fails
	 */
	private java.time.OffsetDateTime parseDateTime(Object value) {
		if (value == null) return null;
		try {
			if (value instanceof String) {
				// Try parsing as OffsetDateTime first
				return java.time.OffsetDateTime.parse((String) value).withOffsetSameInstant(java.time.ZoneOffset.UTC);
			}
			return null;
		} catch (Exception e) {
			log.warn("Failed to parse datetime: {}", value, e);
			return null;
		}
	}

	/**
	 * Hard delete user (complete delete) from content-service admin endpoint
	 */
	public void hardDeleteUser(Long userId) {
		try {
			log.debug("Calling content-service admin endpoint to hard delete user: userId={}", userId);
			getWebClient()
					.delete()
					.uri("/api/v1/admin/content/users/{userId}/hard-delete", userId)
					.retrieve()
					.bodyToMono(Void.class)
					.block();
			log.info("User hard deleted from content-service successfully: userId={}", userId);
		} catch (Exception e) {
			log.error("Error calling content-service to hard delete user: userId={}", userId, e);
			throw new RuntimeException("Failed to hard delete user from content-service: " + e.getMessage(), e);
		}
	}
}

