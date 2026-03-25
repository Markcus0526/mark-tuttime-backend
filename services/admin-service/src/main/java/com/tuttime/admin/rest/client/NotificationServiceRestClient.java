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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

/**
 * REST Client for Notification Service
 *
 * <p>Used for admin operations that require REST API calls to notification-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceRestClient {

	private final WebClient.Builder webClientBuilder;

	@Value("${services.notification-service.url:http://localhost:28088}")
	private String notificationServiceUrl;

	private WebClient getWebClient() {
		return webClientBuilder.clone()
				.baseUrl(notificationServiceUrl)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	/**
	 * List all notifications with optional status filter
	 *
	 * @param pageable Pagination parameters
	 * @param status   Optional status filter
	 * @return Map representation of Page<Notification>
	 */
	public Map<String, Object> listNotifications(org.springframework.data.domain.Pageable pageable, String status) {
		try {
			log.debug("Calling notification-service to list notifications: page={}, size={}, status={}",
					pageable.getPageNumber(), pageable.getPageSize(), status);

			StringBuilder uriBuilder = new StringBuilder("/api/admin/notifications");
			boolean hasParams = false;
			if (status != null && !status.isEmpty()) {
				uriBuilder.append("?").append("status=").append(status);
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
				uriBuilder.append(hasParams ? "&" : "?")
						.append("page=0&size=50");
			}

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uriBuilder.toString())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				Map<String, Object> pageData = response.getData();
				log.debug("Received page data keys: {}", pageData.keySet());

				// Ensure all required fields are present with defaults
				if (!pageData.containsKey("content") || pageData.get("content") == null) {
					pageData.put("content", new java.util.ArrayList<>());
				}
				if (!pageData.containsKey("totalElements") || pageData.get("totalElements") == null) {
					pageData.put("totalElements", 0L);
				}
				if (!pageData.containsKey("totalPages") || pageData.get("totalPages") == null) {
					pageData.put("totalPages", 0);
				}
				if (!pageData.containsKey("number") || pageData.get("number") == null) {
					pageData.put("number", pageable != null ? pageable.getPageNumber() : 0);
				}
				if (!pageData.containsKey("size") || pageData.get("size") == null) {
					pageData.put("size", pageable != null ? pageable.getPageSize() : 50);
				}
				log.debug("Page data after defaults: totalElements={}, totalPages={}, number={}, size={}",
						pageData.get("totalElements"), pageData.get("totalPages"),
						pageData.get("number"), pageData.get("size"));
				return pageData;
			}

			// Return empty page if no data
			Map<String, Object> emptyPage = new java.util.HashMap<>();
			emptyPage.put("content", new java.util.ArrayList<>());
			emptyPage.put("totalElements", 0L);
			emptyPage.put("totalPages", 0);
			emptyPage.put("number", pageable != null ? pageable.getPageNumber() : 0);
			emptyPage.put("size", pageable != null ? pageable.getPageSize() : 50);
			return emptyPage;
		} catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
			// Connection refused or service unavailable - return empty page
			if (e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
					e.getMessage().contains("Connection timed out") ||
					e.getMessage().contains("Service Unavailable"))) {
				log.warn("Notification-service is not available (connection refused/timeout). Returning empty notifications list. URL: {}", notificationServiceUrl);
				Map<String, Object> emptyPage = new java.util.HashMap<>();
				emptyPage.put("content", new java.util.ArrayList<>());
				emptyPage.put("totalElements", 0L);
				emptyPage.put("totalPages", 0);
				emptyPage.put("number", pageable != null ? pageable.getPageNumber() : 0);
				emptyPage.put("size", pageable != null ? pageable.getPageSize() : 50);
				return emptyPage;
			}
			log.error("Network error listing notifications: page={}, size={}, status={}",
					pageable.getPageNumber(), pageable.getPageSize(), status, e);
			throw new RuntimeException("Failed to list notifications: " + e.getMessage(), e);
		} catch (Exception e) {
			// Check if it's a connection error
			if (e.getCause() instanceof java.net.ConnectException ||
					e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
							e.getMessage().contains("Connection timed out"))) {
				log.warn("Notification-service is not available. Returning empty notifications list. URL: {}", notificationServiceUrl);
				Map<String, Object> emptyPage = new java.util.HashMap<>();
				emptyPage.put("content", new java.util.ArrayList<>());
				emptyPage.put("totalElements", 0L);
				emptyPage.put("totalPages", 0);
				emptyPage.put("number", pageable != null ? pageable.getPageNumber() : 0);
				emptyPage.put("size", pageable != null ? pageable.getPageSize() : 50);
				return emptyPage;
			}
			log.error("REST error listing notifications: page={}, size={}, status={}",
					pageable.getPageNumber(), pageable.getPageSize(), status, e);
			throw new RuntimeException("Failed to list notifications: " + e.getMessage(), e);
		}
	}

	/**
	 * Get notification by ID
	 *
	 * @param id Notification ID
	 * @return Map representation of Notification
	 */
	public Map<String, Object> getNotificationById(Long id) {
		try {
			log.debug("Calling notification-service to get notification: {}", id);
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/notifications/{id}", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Notification not found: " + id);
		} catch (WebClientResponseException.NotFound e) {
			throw new RuntimeException("Notification not found: " + id, e);
		} catch (Exception e) {
			log.error("REST error getting notification: id={}", id, e);
			throw new RuntimeException("Failed to get notification: " + e.getMessage(), e);
		}
	}

	/**
	 * Create a new notification
	 *
	 * @param request Notification request data
	 * @return Map representation of created Notification
	 */
	public Map<String, Object> createNotification(Map<String, Object> request) {
		try {
			log.debug("Calling notification-service to create notification");
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/notifications")
					.bodyValue(request)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to create notification");
		} catch (Exception e) {
			log.error("REST error creating notification", e);
			throw new RuntimeException("Failed to create notification: " + e.getMessage(), e);
		}
	}

	/**
	 * Update a notification
	 *
	 * @param id      Notification ID
	 * @param request Notification request data
	 * @return Map representation of updated Notification
	 */
	public Map<String, Object> updateNotification(Long id, Map<String, Object> request) {
		try {
			log.debug("Calling notification-service to update notification: {}", id);
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/admin/notifications/{id}", id)
					.bodyValue(request)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to update notification: " + id);
		} catch (Exception e) {
			log.error("REST error updating notification: id={}", id, e);
			throw new RuntimeException("Failed to update notification: " + e.getMessage(), e);
		}
	}

	/**
	 * Delete a notification
	 *
	 * @param id Notification ID
	 */
	public void deleteNotification(Long id) {
		try {
			log.debug("Calling notification-service to delete notification: {}", id);
			getWebClient()
					.delete()
					.uri("/api/admin/notifications/{id}", id)
					.retrieve()
					.bodyToMono(Void.class)
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();
			log.info("Notification deleted successfully: {}", id);
		} catch (Exception e) {
			log.error("REST error deleting notification: id={}", id, e);
			throw new RuntimeException("Failed to delete notification: " + e.getMessage(), e);
		}
	}

	/**
	 * Publish a notification immediately
	 *
	 * @param id Notification ID
	 * @return Map representation of published Notification
	 */
	public Map<String, Object> publishNotification(Long id) {
		try {
			log.debug("Calling notification-service to publish notification: {}", id);
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/notifications/{id}/publish", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to publish notification: " + id);
		} catch (Exception e) {
			log.error("REST error publishing notification: id={}", id, e);
			throw new RuntimeException("Failed to publish notification: " + e.getMessage(), e);
		}
	}

	/**
	 * Cancel a scheduled notification
	 *
	 * @param id Notification ID
	 * @return Map representation of cancelled Notification
	 */
	public Map<String, Object> cancelNotification(Long id) {
		try {
			log.debug("Calling notification-service to cancel notification: {}", id);
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/notifications/{id}/cancel", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to cancel notification: " + id);
		} catch (Exception e) {
			log.error("REST error canceling notification: id={}", id, e);
			throw new RuntimeException("Failed to cancel notification: " + e.getMessage(), e);
		}
	}

	/**
	 * Schedule a notification for future sending
	 *
	 * @param id          Notification ID
	 * @param scheduledAt Scheduled time
	 * @return Map representation of scheduled Notification
	 */
	public Map<String, Object> scheduleNotification(Long id, java.time.OffsetDateTime scheduledAt) {
		try {
			log.debug("Calling notification-service to schedule notification: {} for {}", id, scheduledAt);
			Map<String, Object> requestBody = Map.of("scheduledAt", scheduledAt.toString());
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/notifications/{id}/schedule", id)
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to schedule notification: " + id);
		} catch (Exception e) {
			log.error("REST error scheduling notification: id={}", id, e);
			throw new RuntimeException("Failed to schedule notification: " + e.getMessage(), e);
		}
	}

	/**
	 * Hard delete user (complete delete) from notification-service admin endpoint
	 */
	public void hardDeleteUser(Long userId) {
		try {
			log.debug("Calling notification-service admin endpoint to hard delete user: userId={}", userId);
			getWebClient()
					.delete()
					.uri("/api/v1/admin/notifications/users/{userId}/hard-delete", userId)
					.retrieve()
					.bodyToMono(Void.class)
					.block();
			log.info("User hard deleted from notification-service successfully: userId={}", userId);
		} catch (Exception e) {
			log.error("Error calling notification-service to hard delete user: userId={}", userId, e);
			throw new RuntimeException("Failed to hard delete user from notification-service: " + e.getMessage(), e);
		}
	}

	/**
	 * Send push notification to a user
	 *
	 * @param targetUserId Target user ID
	 * @param title        Notification title
	 * @param body         Notification body
	 * @param data         Additional data payload (optional)
	 * @param priority     Notification priority (optional)
	 * @return Map representation of push notification result
	 */
	public Map<String, Object> sendPushNotification(
			Long targetUserId,
			String title,
			String body,
			Map<String, String> data,
			String priority) {
		try {
			log.debug("Calling notification-service to send push notification: targetUserId={}, title={}",
					targetUserId, title);

			Map<String, Object> requestBody = new java.util.HashMap<>();
			requestBody.put("targetUserId", targetUserId);
			requestBody.put("title", title);
			requestBody.put("body", body);
			if (data != null && !data.isEmpty()) {
				requestBody.put("data", data);
			}
			if (priority != null && !priority.isEmpty()) {
				requestBody.put("priority", priority);
			}

			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/v1/notifications/push/send")
					.header("X-User-Id", "0") // System user ID for admin operations
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to send push notification");
		} catch (Exception e) {
			log.error("REST error sending push notification: targetUserId={}, title={}", targetUserId, title, e);
			throw new RuntimeException("Failed to send push notification: " + e.getMessage(), e);
		}
	}

	// ========== Template Management ==========

	public Map<String, Object> listTemplates(org.springframework.data.domain.Pageable pageable, String category) {
		try {
			StringBuilder uri = new StringBuilder("/api/admin/notifications/templates");
			boolean hasParams = false;
			if (category != null && !category.isEmpty()) {
				uri.append("?category=").append(category);
				hasParams = true;
			}
			if (pageable != null) {
				uri.append(hasParams ? "&" : "?")
						.append("page=").append(pageable.getPageNumber())
						.append("&size=").append(pageable.getPageSize());
			}

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uri.toString())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			return response != null && response.getData() != null ? response.getData() : createEmptyPage(pageable);
		} catch (Exception e) {
			log.error("REST error listing templates", e);
			return createEmptyPage(pageable);
		}
	}

	public Map<String, Object> getTemplate(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/notifications/templates/{id}", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Template not found: " + id);
		} catch (Exception e) {
			log.error("REST error getting template: id={}", id, e);
			throw new RuntimeException("Failed to get template: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> getTemplateByKey(String key) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/notifications/templates/key/{key}", key)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Template not found: " + key);
		} catch (Exception e) {
			log.error("REST error getting template by key: key={}", key, e);
			throw new RuntimeException("Failed to get template: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> createTemplate(Map<String, Object> request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/notifications/templates")
					.bodyValue(request)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to create template");
		} catch (Exception e) {
			log.error("REST error creating template", e);
			throw new RuntimeException("Failed to create template: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> updateTemplate(Long id, Map<String, Object> request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/admin/notifications/templates/{id}", id)
					.bodyValue(request)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to update template: " + id);
		} catch (Exception e) {
			log.error("REST error updating template: id={}", id, e);
			throw new RuntimeException("Failed to update template: " + e.getMessage(), e);
		}
	}

	public void deleteTemplate(Long id) {
		try {
			getWebClient()
					.delete()
					.uri("/api/admin/notifications/templates/{id}", id)
					.retrieve()
					.bodyToMono(Void.class)
					.block();
		} catch (Exception e) {
			log.error("REST error deleting template: id={}", id, e);
			throw new RuntimeException("Failed to delete template: " + e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public java.util.List<Map<String, Object>> getTemplateTranslations(Long templateId) {
		try {
			ApiResponse<java.util.List<Map<String, Object>>> response = getWebClient()
					.get()
					.uri("/api/admin/notifications/templates/{id}/translations", templateId)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<java.util.List<Map<String, Object>>>>() {
					})
					.block();

			return response != null && response.getData() != null ? response.getData() : new java.util.ArrayList<>();
		} catch (Exception e) {
			log.error("REST error getting template translations: templateId={}", templateId, e);
			return new java.util.ArrayList<>();
		}
	}

	public Map<String, Object> updateTranslation(Long templateId, String languageCode, Map<String, Object> request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/admin/notifications/templates/{id}/translations/{languageCode}", templateId, languageCode)
					.bodyValue(request)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to update translation");
		} catch (Exception e) {
			log.error("REST error updating translation: templateId={}, language={}", templateId, languageCode, e);
			throw new RuntimeException("Failed to update translation: " + e.getMessage(), e);
		}
	}

	// ========== Campaign Management ==========

	public Map<String, Object> listCampaigns(org.springframework.data.domain.Pageable pageable, String status) {
		try {
			StringBuilder uri = new StringBuilder("/api/admin/notifications/campaigns");
			boolean hasParams = false;
			if (status != null && !status.isEmpty()) {
				uri.append("?status=").append(status);
				hasParams = true;
			}
			if (pageable != null) {
				uri.append(hasParams ? "&" : "?")
						.append("page=").append(pageable.getPageNumber())
						.append("&size=").append(pageable.getPageSize());
			}

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uri.toString())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			return response != null && response.getData() != null ? response.getData() : createEmptyPage(pageable);
		} catch (Exception e) {
			log.error("REST error listing campaigns", e);
			return createEmptyPage(pageable);
		}
	}

	public Map<String, Object> getCampaign(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/notifications/campaigns/{id}", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Campaign not found: " + id);
		} catch (Exception e) {
			log.error("REST error getting campaign: id={}", id, e);
			throw new RuntimeException("Failed to get campaign: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> createCampaign(Map<String, Object> request) {
		try {
			log.debug("Calling notification-service to create campaign: request={}", request);

			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/notifications/campaigns")
					.bodyValue(request)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
							.filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable))
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to create campaign: response data is null");
		} catch (WebClientResponseException e) {
			// Extract error message from response body if available
			String errorMessage = "Failed to create campaign";
			String responseBody = e.getResponseBodyAsString();

			log.error("Notification service returned error: status={}, statusText={}, responseBody={}",
					e.getStatusCode(), e.getStatusText(), responseBody);

			try {
				if (responseBody != null && !responseBody.isEmpty()) {
					// Try to parse as ErrorResponse to get the message
					com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
					java.util.Map<String, Object> errorMap = mapper.readValue(responseBody, java.util.Map.class);
					String message = (String) errorMap.get("message");
					if (message != null && !message.isEmpty()) {
						errorMessage = message;
					} else {
						// Fallback to error field if message is not available
						String error = (String) errorMap.get("error");
						if (error != null && !error.isEmpty()) {
							errorMessage = "Failed to create campaign: " + error;
						}
					}
				} else {
					errorMessage = String.format("Failed to create campaign: %d %s",
							e.getStatusCode().value(), e.getStatusText());
				}
			} catch (Exception parseEx) {
				log.debug("Could not parse error response as JSON, using status code", parseEx);
				errorMessage = String.format("Failed to create campaign: %d %s",
						e.getStatusCode().value(), e.getStatusText());
			}

			throw new RuntimeException(errorMessage, e);
		} catch (Exception e) {
			log.error("REST error creating campaign: request={}, error={}", request, e.getMessage(), e);
			throw new RuntimeException("Failed to create campaign: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> updateCampaign(Long id, Map<String, Object> request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/admin/notifications/campaigns/{id}", id)
					.bodyValue(request)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to update campaign: " + id);
		} catch (Exception e) {
			log.error("REST error updating campaign: id={}", id, e);
			throw new RuntimeException("Failed to update campaign: " + e.getMessage(), e);
		}
	}

	public void deleteCampaign(Long id) {
		try {
			getWebClient()
					.delete()
					.uri("/api/admin/notifications/campaigns/{id}", id)
					.retrieve()
					.bodyToMono(Void.class)
					.block();
		} catch (Exception e) {
			log.error("REST error deleting campaign: id={}", id, e);
			throw new RuntimeException("Failed to delete campaign: " + e.getMessage(), e);
		}
	}

	public void cancelCampaign(Long id) {
		try {
			getWebClient()
					.post()
					.uri("/api/admin/notifications/campaigns/{id}/cancel", id)
					.retrieve()
					.bodyToMono(Void.class)
					.block();
		} catch (Exception e) {
			log.error("REST error cancelling campaign: id={}", id, e);
			throw new RuntimeException("Failed to cancel campaign: " + e.getMessage(), e);
		}
	}

	// ========== User Group Management ==========

	public Map<String, Object> listUserGroups(org.springframework.data.domain.Pageable pageable) {
		try {
			StringBuilder uri = new StringBuilder("/api/admin/notifications/user-groups");
			if (pageable != null) {
				uri.append("?page=").append(pageable.getPageNumber())
						.append("&size=").append(pageable.getPageSize());
			}

			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri(uri.toString())
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			return response != null && response.getData() != null ? response.getData() : createEmptyPage(pageable);
		} catch (Exception e) {
			log.error("REST error listing user groups", e);
			return createEmptyPage(pageable);
		}
	}

	public Map<String, Object> getUserGroup(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.get()
					.uri("/api/admin/notifications/user-groups/{id}", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("User group not found: " + id);
		} catch (Exception e) {
			log.error("REST error getting user group: id={}", id, e);
			throw new RuntimeException("Failed to get user group: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> createUserGroup(Map<String, Object> request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/notifications/user-groups")
					.bodyValue(request)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to create user group");
		} catch (Exception e) {
			log.error("REST error creating user group", e);
			throw new RuntimeException("Failed to create user group: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> updateUserGroup(Long id, Map<String, Object> request) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.put()
					.uri("/api/admin/notifications/user-groups/{id}", id)
					.bodyValue(request)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to update user group: " + id);
		} catch (Exception e) {
			log.error("REST error updating user group: id={}", id, e);
			throw new RuntimeException("Failed to update user group: " + e.getMessage(), e);
		}
	}

	public void deleteUserGroup(Long id) {
		try {
			getWebClient()
					.delete()
					.uri("/api/admin/notifications/user-groups/{id}", id)
					.retrieve()
					.bodyToMono(Void.class)
					.block();
		} catch (Exception e) {
			log.error("REST error deleting user group: id={}", id, e);
			throw new RuntimeException("Failed to delete user group: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> refreshUserGroupMembers(Long id) {
		try {
			ApiResponse<Map<String, Object>> response = getWebClient()
					.post()
					.uri("/api/admin/notifications/user-groups/{id}/refresh", id)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
					})
					.block();

			if (response != null && response.getData() != null) {
				return response.getData();
			}
			throw new RuntimeException("Failed to refresh user group members: " + id);
		} catch (Exception e) {
			log.error("REST error refreshing user group members: id={}", id, e);
			throw new RuntimeException("Failed to refresh user group members: " + e.getMessage(), e);
		}
	}

	// Helper method to create empty page
	private Map<String, Object> createEmptyPage(org.springframework.data.domain.Pageable pageable) {
		Map<String, Object> emptyPage = new java.util.HashMap<>();
		emptyPage.put("content", new java.util.ArrayList<>());
		emptyPage.put("totalElements", 0L);
		emptyPage.put("totalPages", 0);
		emptyPage.put("number", pageable != null ? pageable.getPageNumber() : 0);
		emptyPage.put("size", pageable != null ? pageable.getPageSize() : 50);
		return emptyPage;
	}
}
