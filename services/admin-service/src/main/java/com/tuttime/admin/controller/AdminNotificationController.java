package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.kafka.CampaignEventProducer;
import com.tuttime.admin.rest.client.MiningServiceRestClient;
import com.tuttime.admin.rest.client.NotificationServiceRestClient;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminUserManagementService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Admin Notification Management Controller
 *
 * <p>Provides endpoints for managing notifications:
 * creating, updating, deleting, scheduling, publishing, and canceling notifications.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notifications")
@Tag(name = "Admin Notification Management", description = "Admin APIs for notification management")
public class AdminNotificationController {

	private final NotificationServiceRestClient notificationServiceRestClient;
	private final AdminAuditService auditService;
	private final AdminUserManagementService adminUserManagementService;
	private final MiningServiceRestClient miningServiceRestClient;
	private final CampaignEventProducer campaignEventProducer; // Optional - only if Kafka is enabled

	@org.springframework.beans.factory.annotation.Autowired(required = false)
	@Qualifier("asyncOperationExecutor")
	private Executor asyncOperationExecutor;

	@org.springframework.beans.factory.annotation.Autowired(required = false)
	private RedisTemplate<String, String> redisTemplate;

	private static final String REDIS_KEY_LAST_INACTIVE_NOTIFICATION = "mining:notification:inactive:last_sent_at";
	private static final String REDIS_KEY_LAST_NEVER_STARTED_NOTIFICATION = "mining:notification:never_started:last_sent_at";

	// Constructor for required dependencies
	public AdminNotificationController(
			NotificationServiceRestClient notificationServiceRestClient,
			AdminAuditService auditService,
			AdminUserManagementService adminUserManagementService,
			MiningServiceRestClient miningServiceRestClient,
			@org.springframework.beans.factory.annotation.Autowired(required = false) CampaignEventProducer campaignEventProducer) {

		this.notificationServiceRestClient = notificationServiceRestClient;
		this.auditService = auditService;
		this.adminUserManagementService = adminUserManagementService;
		this.miningServiceRestClient = miningServiceRestClient;
		this.campaignEventProducer = campaignEventProducer;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List Notifications", description = "Get list of all notifications with optional status filter")
	public ResponseEntity<ApiResponse<Map<String, Object>>> listNotifications(
			@PageableDefault(size = 50) Pageable pageable,
			@RequestParam(required = false) String status,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Listing notifications, status filter: {}", adminId, status);

		Map<String, Object> pageData = notificationServiceRestClient.listNotifications(pageable, status);

		// Ensure all required pagination fields are present and properly formatted
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> content = pageData.get("content") != null
				? (List<Map<String, Object>>) pageData.get("content")
				: new java.util.ArrayList<>();

		Object totalElementsObj = pageData.get("totalElements");
		long totalElements = totalElementsObj != null
				? ((Number) totalElementsObj).longValue()
				: 0L;

		Object totalPagesObj = pageData.get("totalPages");
		int totalPages = totalPagesObj != null
				? ((Number) totalPagesObj).intValue()
				: 0;

		Object numberObj = pageData.get("number");
		int number = numberObj != null
				? ((Number) numberObj).intValue()
				: (pageable != null ? pageable.getPageNumber() : 0);

		Object sizeObj = pageData.get("size");
		int size = sizeObj != null
				? ((Number) sizeObj).intValue()
				: (pageable != null ? pageable.getPageSize() : 50);

		// Create properly formatted page map for consistent serialization
		Map<String, Object> pageMap = new java.util.HashMap<>();
		pageMap.put("content", content);
		pageMap.put("totalElements", totalElements);
		pageMap.put("totalPages", totalPages);
		pageMap.put("number", number);
		pageMap.put("size", size);
		pageMap.put("numberOfElements", content.size());
		pageMap.put("first", number == 0);
		pageMap.put("last", totalPages == 0 || number >= totalPages - 1);
		pageMap.put("empty", content.isEmpty());
		pageMap.put("pageable", Map.of(
				"pageNumber", number,
				"pageSize", size,
				"paged", true
		));

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_VIEWED,
				"NOTIFICATION", null, "Listed notifications");

		return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", pageMap));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get Notification", description = "Get notification details")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getNotification(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Getting notification: {}", adminId, id);

		Map<String, Object> notification = notificationServiceRestClient.getNotificationById(id);

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_VIEWED,
				"NOTIFICATION", id, "Viewed notification: " + id);

		return ResponseEntity.ok(ApiResponse.success("Notification retrieved successfully", notification));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Create Notification", description = "Create a new notification")
	public ResponseEntity<ApiResponse<Map<String, Object>>> createNotification(
			@Valid @RequestBody CreateNotificationRequest request,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Creating notification: titleEn={}, targetAudience={}",
				adminId, request.getTitleEn(), request.getTargetAudience());

		// Validate new fields are provided
		if (request.getTitleEn() == null || request.getTitleEn().trim().isEmpty()) {
			throw new IllegalArgumentException("English title (titleEn) is required");
		}
		if (request.getMessageEn() == null || request.getMessageEn().trim().isEmpty()) {
			throw new IllegalArgumentException("English message (messageEn) is required");
		}
		if (request.getTitleRu() == null || request.getTitleRu().trim().isEmpty()) {
			throw new IllegalArgumentException("Russian title (titleRu) is required");
		}
		if (request.getMessageRu() == null || request.getMessageRu().trim().isEmpty()) {
			throw new IllegalArgumentException("Russian message (messageRu) is required");
		}

		// Convert to notification-service format
		// Use new fields (required), fall back to old fields only for backward compatibility
		String titleEn = request.getTitleEn() != null ? request.getTitleEn() : request.getTitle();
		String messageEn = request.getMessageEn() != null ? request.getMessageEn() : request.getMessage();

		Map<String, Object> notificationRequest = new HashMap<>();
		notificationRequest.put("notificationType", request.getNotificationType() != null ? request.getNotificationType() : "CUSTOM_NOTIFICATION");
		notificationRequest.put("channel", request.getChannel() != null ? request.getChannel() : "IN_APP");
		notificationRequest.put("subject", titleEn); // Use English for backward compatibility
		notificationRequest.put("message", messageEn); // Use English for backward compatibility
		notificationRequest.put("subjectEn", titleEn);
		notificationRequest.put("messageEn", messageEn);
		notificationRequest.put("subjectRu", request.getTitleRu());
		notificationRequest.put("messageRu", request.getMessageRu());
		notificationRequest.put("priority", request.getPriority() != null ? request.getPriority() : "NORMAL");
		notificationRequest.put("targetAudience", request.getTargetAudience());
		notificationRequest.put("scheduledAt", request.getScheduledAt());
		notificationRequest.put("userIds", request.getUserIds());
		notificationRequest.put("referenceType", request.getReferenceType());
		notificationRequest.put("referenceId", request.getReferenceId());

		// Store English/Russian in metadata for translation
		Map<String, Object> metadata = request.getMetadata() != null ? new HashMap<>(request.getMetadata()) : new HashMap<>();
		metadata.put("titleEn", titleEn);
		metadata.put("messageEn", messageEn);
		metadata.put("titleRu", request.getTitleRu());
		metadata.put("messageRu", request.getMessageRu());
		notificationRequest.put("metadata", metadata);

		Map<String, Object> notification = notificationServiceRestClient.createNotification(notificationRequest);

		Long notificationId = null;
		if (notification.get("notificationId") != null) {
			Object idObj = notification.get("notificationId");
			if (idObj instanceof Number) {
				notificationId = ((Number) idObj).longValue();
			}
		}
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_CREATED,
				"NOTIFICATION", notificationId,
				"Created notification: " + request.getTitle());

		return ResponseEntity.ok(ApiResponse.success("Notification created successfully", notification));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update Notification", description = "Update notification")
	public ResponseEntity<ApiResponse<Map<String, Object>>> updateNotification(
			@PathVariable Long id,
			@Valid @RequestBody UpdateNotificationRequest request,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Updating notification: {}", adminId, id);

		// Convert to notification-service format
		Map<String, Object> notificationRequest = new HashMap<>();
		if (request.getNotificationType() != null) {
			notificationRequest.put("notificationType", request.getNotificationType());
		}
		if (request.getChannel() != null) {
			notificationRequest.put("channel", request.getChannel());
		}
		if (request.getTitle() != null) {
			notificationRequest.put("subject", request.getTitle());
		}
		if (request.getMessage() != null) {
			notificationRequest.put("message", request.getMessage());
		}
		if (request.getPriority() != null) {
			notificationRequest.put("priority", request.getPriority());
		}
		if (request.getTargetAudience() != null) {
			notificationRequest.put("targetAudience", request.getTargetAudience());
		}
		if (request.getScheduledAt() != null) {
			notificationRequest.put("scheduledAt", request.getScheduledAt());
		}
		if (request.getUserIds() != null) {
			notificationRequest.put("userIds", request.getUserIds());
		}
		if (request.getReferenceType() != null) {
			notificationRequest.put("referenceType", request.getReferenceType());
		}
		if (request.getReferenceId() != null) {
			notificationRequest.put("referenceId", request.getReferenceId());
		}
		if (request.getMetadata() != null) {
			notificationRequest.put("metadata", request.getMetadata());
		}

		Map<String, Object> notification = notificationServiceRestClient.updateNotification(id, notificationRequest);

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_UPDATED,
				"NOTIFICATION", id, "Updated notification: " + id);

		return ResponseEntity.ok(ApiResponse.success("Notification updated successfully", notification));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Delete Notification", description = "Delete notification")
	public ResponseEntity<ApiResponse<Void>> deleteNotification(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Deleting notification: {}", adminId, id);

		notificationServiceRestClient.deleteNotification(id);

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_DELETED,
				"NOTIFICATION", id, "Deleted notification: " + id);

		return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
	}

	@PostMapping("/{id}/publish")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Publish Notification", description = "Publish notification immediately")
	public ResponseEntity<ApiResponse<Map<String, Object>>> publishNotification(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Publishing notification: {}", adminId, id);

		Map<String, Object> notification = notificationServiceRestClient.publishNotification(id);

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_PUBLISHED,
				"NOTIFICATION", id, "Published notification: " + id);

		return ResponseEntity.ok(ApiResponse.success("Notification published successfully", notification));
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Cancel Notification", description = "Cancel scheduled notification")
	public ResponseEntity<ApiResponse<Map<String, Object>>> cancelNotification(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Canceling notification: {}", adminId, id);

		Map<String, Object> notification = notificationServiceRestClient.cancelNotification(id);

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_CANCELLED,
				"NOTIFICATION", id, "Cancelled notification: " + id);

		return ResponseEntity.ok(ApiResponse.success("Notification cancelled successfully", notification));
	}

	@PostMapping("/{id}/schedule")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Schedule Notification", description = "Schedule notification for future")
	public ResponseEntity<ApiResponse<Map<String, Object>>> scheduleNotification(
			@PathVariable Long id,
			@Valid @RequestBody ScheduleNotificationRequest request,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Scheduling notification: {} for {}", adminId, id, request.getScheduledAt());

		Map<String, Object> notification = notificationServiceRestClient.scheduleNotification(id, request.getScheduledAt());

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_SCHEDULED,
				"NOTIFICATION", id, "Scheduled notification: " + id + " for " + request.getScheduledAt());

		return ResponseEntity.ok(ApiResponse.success("Notification scheduled successfully", notification));
	}

	@PostMapping("/campaigns")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Create Notification Campaign",
			description = "Creates a new notification campaign and publishes to Kafka for async processing. Uses event-driven architecture for high-performance bulk notifications.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> createCampaign(
			@Valid @RequestBody CreateCampaignRequest request,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Creating campaign: name={}, targetAudience={}",
				adminId, request.getCampaignName(), request.getTargetAudience());

		// 1. Save campaign to notification-service DB first (via REST)
		Map<String, Object> campaignRequest = new HashMap<>();
		campaignRequest.put("campaignName", request.getCampaignName());
		campaignRequest.put("templateKey", request.getTemplateKey());
		// Include deprecated fields for backward compatibility (notification service still requires them)
		campaignRequest.put("sourceTitle", request.getSourceTitle() != null ? request.getSourceTitle() : request.getSourceTitleEn());
		campaignRequest.put("sourceMessage", request.getSourceMessage() != null ? request.getSourceMessage() : request.getSourceMessageEn());
		campaignRequest.put("sourceTitleEn", request.getSourceTitleEn());
		campaignRequest.put("sourceMessageEn", request.getSourceMessageEn());
		campaignRequest.put("sourceTitleRu", request.getSourceTitleRu());
		campaignRequest.put("sourceMessageRu", request.getSourceMessageRu());
		campaignRequest.put("sourceLanguage", request.getSourceLanguage() != null ? request.getSourceLanguage() : "en");
		campaignRequest.put("channels", request.getChannels());
		campaignRequest.put("targetAudience", request.getTargetAudience());
		campaignRequest.put("targetUserGroupId", request.getTargetUserGroupId());
		campaignRequest.put("targetCriteria", request.getTargetCriteria());
		campaignRequest.put("priority", request.getPriority() != null ? request.getPriority() : "NORMAL");
		campaignRequest.put("status", "DRAFT");
		campaignRequest.put("createdBy", adminId);

		Map<String, Object> campaign = notificationServiceRestClient.createCampaign(campaignRequest);
		Long campaignId = ((Number) campaign.get("campaignId")).longValue();

		log.info("Admin {}: Campaign saved to DB: campaignId={}", adminId, campaignId);

		// 2. Publish to Kafka for async processing
		if (campaignEventProducer != null) {
			campaignEventProducer.sendCampaignCreated(
					campaignId,
					request.getCampaignName(),
					request.getTemplateKey(),
					"en", // Always use English as source for translation
					request.getSourceTitleEn(), // For backward compatibility
					request.getSourceMessageEn(), // For backward compatibility
					request.getSourceTitleEn(),
					request.getSourceMessageEn(),
					request.getSourceTitleRu(),
					request.getSourceMessageRu(),
					request.getChannels(),
					request.getTargetAudience(),
					request.getTargetUserGroupId(),
					request.getTargetCriteria(),
					request.getPriority(),
					adminId
			);

			log.info("Admin {}: Campaign event published to Kafka: campaignId={}, campaignName={}",
					adminId, campaignId, request.getCampaignName());

			auditService.logAction(adminId, null,
					com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_CREATED,
					"CAMPAIGN", campaignId,
					"Created campaign: " + request.getCampaignName());

			return ResponseEntity.ok(ApiResponse.success(
					"Campaign created successfully and queued for processing", campaign));
		} else {
			// Fallback: Campaign already saved, just return it
			log.warn("Kafka not available, campaign saved but not queued for processing");
			return ResponseEntity.ok(ApiResponse.success(
					"Campaign created successfully (Kafka not configured, manual processing required)", campaign));
		}
	}

	@PostMapping("/push/inactive-users")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Send push notification to all users with inactive mining sessions (Legacy)",
			description = "Legacy endpoint - use /campaigns instead. Sends push notification to all users whose mining sessions are inactive (miningActive = false). Processes users in batches asynchronously. Returns immediately with job started confirmation. Request body is optional - defaults will be used if not provided. Can be called with no body, empty body {}, or partial body with only priority/data fields.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> sendPushToInactiveUsers(
			@RequestBody(required = false) SendPushToInactiveUsersRequest request,
			@AuthenticationPrincipal Long adminId) {

		// Use default values if request is null (no body sent) or empty (empty JSON {})
		// All fields (title, body, priority, data) are optional - defaults will be used
		if (request == null) {
			request = SendPushToInactiveUsersRequest.builder().build();
		}

		// Template content from notification service templates
		// Template: admin_inactive_mining_users
		final String templateKey = "admin_inactive_mining_users";
		final String templateTitleEn = "Come Back to Mining!";
		final String templateMessageEn = "We noticed you haven't been mining lately. Start mining again to earn AP Credits and unlock amazing rewards!";
		final String templateTitleRu = "Вернитесь к майнингу!";
		final String templateMessageRu = "Мы заметили, что вы давно не занимались майнингом. Начните майнинг снова, чтобы заработать AP Credits и разблокировать удивительные награды!";

		// Use custom text if provided, otherwise use template content
		final String notificationTitleEn = (request.getTitle() != null && !request.getTitle().trim().isEmpty())
				? request.getTitle()
				: templateTitleEn;
		final String notificationBodyEn = (request.getBody() != null && !request.getBody().trim().isEmpty())
				? request.getBody()
				: templateMessageEn;

		// For Russian: use custom translation if custom text provided, otherwise use template translation
		String notificationTitleRu = (request.getTitle() != null && !request.getTitle().trim().isEmpty())
				? translateToRussian(request.getTitle())
				: templateTitleRu;
		String notificationBodyRu = (request.getBody() != null && !request.getBody().trim().isEmpty())
				? translateToRussian(request.getBody())
				: templateMessageRu;

		// Ensure Russian translations are not empty (fallback to template if translation failed)
		if (notificationTitleRu == null || notificationTitleRu.trim().isEmpty()) {
			notificationTitleRu = templateTitleRu;
		}
		if (notificationBodyRu == null || notificationBodyRu.trim().isEmpty()) {
			notificationBodyRu = templateMessageRu;
		}

		// Ensure English fields are not empty (fallback to template if somehow empty)
		final String finalTitleEn = (notificationTitleEn != null && !notificationTitleEn.trim().isEmpty())
				? notificationTitleEn
				: templateTitleEn;
		final String finalBodyEn = (notificationBodyEn != null && !notificationBodyEn.trim().isEmpty())
				? notificationBodyEn
				: templateMessageEn;

		log.info("Admin {}: Using legacy endpoint - consider using /campaigns instead. Publishing Kafka event for inactive mining users campaign: titleEn={}, bodyEn={}, titleRu={}, bodyRu={}",
				adminId, finalTitleEn, finalBodyEn, notificationTitleRu, notificationBodyRu);

		// Always use Kafka event approach - notification service will handle pagination and batch processing
		if (campaignEventProducer == null) {
			log.error("Admin {}: Kafka is not available. Cannot send push notifications to inactive mining users.", adminId);
			return ResponseEntity.status(503).body(ApiResponse.error(
					"Kafka is not available. Push notifications require Kafka to be configured."));
		}

		// Create campaign via Kafka using template
		// Note: All source fields are required for validation, even when using templateKey
		// The template will be used for translations, but these serve as fallbacks
		CreateCampaignRequest campaignRequest = CreateCampaignRequest.builder()
				.campaignName("Inactive Mining Users - " + java.time.OffsetDateTime.now())
				.templateKey(templateKey) // Use existing template with translations
				.sourceTitle(finalTitleEn) // Fallback if template not found
				.sourceMessage(finalBodyEn) // Fallback if template not found
				.sourceTitleEn(finalTitleEn) // Required: English title (from template or custom)
				.sourceMessageEn(finalBodyEn) // Required: English message (from template or custom)
				.sourceTitleRu(notificationTitleRu) // Required: Russian title (from template translation or auto-translated)
				.sourceMessageRu(notificationBodyRu) // Required: Russian message (from template translation or auto-translated)
				.sourceLanguage("en")
				.channels(List.of("PUSH"))
				.targetAudience("INACTIVE_MINING")
				.priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
				.build();

		return createCampaign(campaignRequest, adminId);
	}

	@PostMapping("/push/never-started-users")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Send push notification to all users who never started mining (Legacy)",
			description = "Legacy endpoint - use /campaigns instead. Sends push notification to all users who never started mining. Processes users in batches asynchronously. Returns immediately with job started confirmation. Request body is optional - defaults will be used if not provided. Can be called with no body, empty body {}, or partial body with only priority/data fields.")
	public ResponseEntity<ApiResponse<Map<String, Object>>> sendPushToNeverStartedUsers(
			@RequestBody(required = false) SendPushToNeverStartedUsersRequest request,
			@AuthenticationPrincipal Long adminId) {

		// Use default values if request is null (no body sent) or empty (empty JSON {})
		// All fields (title, body, priority, data) are optional - defaults will be used
		if (request == null) {
			request = SendPushToNeverStartedUsersRequest.builder().build();
		}

		// Template content from notification service templates
		// Template: admin_never_started_mining
		final String templateKey = "admin_never_started_mining";
		final String templateTitleEn = "Start Your Mining Journey!";
		final String templateMessageEn = "Welcome! Start mining now to earn AP Credits. It's easy, fun, and rewarding. Begin your journey today!";
		final String templateTitleRu = "Начните свое путешествие в майнинге!";
		final String templateMessageRu = "Добро пожаловать! Начните майнинг прямо сейчас, чтобы заработать AP Credits. Это легко, весело и выгодно. Начните свое путешествие сегодня!";

		// Use custom text if provided, otherwise use template content
		final String notificationTitleEn = (request.getTitle() != null && !request.getTitle().trim().isEmpty())
				? request.getTitle()
				: templateTitleEn;
		final String notificationBodyEn = (request.getBody() != null && !request.getBody().trim().isEmpty())
				? request.getBody()
				: templateMessageEn;

		// For Russian: use custom translation if custom text provided, otherwise use template translation
		String notificationTitleRu = (request.getTitle() != null && !request.getTitle().trim().isEmpty())
				? translateToRussian(request.getTitle())
				: templateTitleRu;
		String notificationBodyRu = (request.getBody() != null && !request.getBody().trim().isEmpty())
				? translateToRussian(request.getBody())
				: templateMessageRu;

		// Ensure Russian translations are not empty (fallback to template if translation failed)
		if (notificationTitleRu == null || notificationTitleRu.trim().isEmpty()) {
			notificationTitleRu = templateTitleRu;
		}
		if (notificationBodyRu == null || notificationBodyRu.trim().isEmpty()) {
			notificationBodyRu = templateMessageRu;
		}

		// Ensure English fields are not empty (fallback to template if somehow empty)
		final String finalTitleEn = (notificationTitleEn != null && !notificationTitleEn.trim().isEmpty())
				? notificationTitleEn
				: templateTitleEn;
		final String finalBodyEn = (notificationBodyEn != null && !notificationBodyEn.trim().isEmpty())
				? notificationBodyEn
				: templateMessageEn;

		log.info("Admin {}: Using legacy endpoint - consider using /campaigns instead. Publishing Kafka event for never-started mining users campaign: titleEn={}, bodyEn={}, titleRu={}, bodyRu={}",
				adminId, finalTitleEn, finalBodyEn, notificationTitleRu, notificationBodyRu);

		// Always use Kafka event approach - notification service will handle pagination and batch processing
		if (campaignEventProducer == null) {
			log.error("Admin {}: Kafka is not available. Cannot send push notifications to never-started mining users.", adminId);
			return ResponseEntity.status(503).body(ApiResponse.error(
					"Kafka is not available. Push notifications require Kafka to be configured."));
		}

		// Create campaign via Kafka using template
		// Note: All source fields are required for validation, even when using templateKey
		// The template will be used for translations, but these serve as fallbacks
		CreateCampaignRequest campaignRequest = CreateCampaignRequest.builder()
				.campaignName("Never Started Mining Users - " + java.time.OffsetDateTime.now())
				.templateKey(templateKey) // Use existing template with translations
				.sourceTitle(finalTitleEn) // Fallback if template not found
				.sourceMessage(finalBodyEn) // Fallback if template not found
				.sourceTitleEn(finalTitleEn) // Required: English title (from template or custom)
				.sourceMessageEn(finalBodyEn) // Required: English message (from template or custom)
				.sourceTitleRu(notificationTitleRu) // Required: Russian title (from template translation or auto-translated)
				.sourceMessageRu(notificationBodyRu) // Required: Russian message (from template translation or auto-translated)
				.sourceLanguage("en")
				.channels(List.of("PUSH"))
				.targetAudience("NEVER_STARTED_MINING")
				.priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
				.build();

		return createCampaign(campaignRequest, adminId);
	}

	/**
	 * Processes push notifications to inactive users asynchronously
	 */
	private void processPushNotifications(
			Long adminId,
			String notificationTitle,
			String notificationBody,
			Map<String, String> dataPayload,
			String priority) {

		log.info("Admin {}: Starting async processing of push notifications to inactive users: title={}, body={}",
				adminId, notificationTitle, notificationBody);

		AtomicInteger totalUsers = new AtomicInteger(0);
		AtomicInteger successfulSends = new AtomicInteger(0);
		AtomicInteger failedSends = new AtomicInteger(0);
		List<Long> failedUserIds = new java.util.ArrayList<>();

		try {
			// Fetch AP balances in batches to find users with inactive mining
			// AP balances include userId and miningActive field
			int pageSize = 1000; // Process 1000 users at a time
			int currentPage = 0;
			boolean hasMore = true;
			int totalProcessed = 0;
			int inactiveCount = 0;

			while (hasMore) {
				Pageable pageable = PageRequest.of(currentPage, pageSize);
				Page<Map<String, Object>> balancesPage = miningServiceRestClient.listApBalances(pageable);

				if (balancesPage.isEmpty()) {
					hasMore = false;
					break;
				}

				List<Map<String, Object>> balances = balancesPage.getContent();
				totalProcessed += balances.size();
				log.info("Processing batch {}: {} AP balances (total processed so far: {})", currentPage + 1, balances.size(), totalProcessed);

				// Debug: Log first balance structure to verify field names
				if (!balances.isEmpty()) {
					Map<String, Object> firstBalance = balances.get(0);
					log.info("Sample balance structure - keys: {}, userId: {}, miningActive: {}, miningActive type: {}",
							firstBalance.keySet(), firstBalance.get("userId"), firstBalance.get("miningActive"),
							firstBalance.get("miningActive") != null ? firstBalance.get("miningActive").getClass().getName() : "null");
				}

				// Filter for users with inactive mining and send push notifications
				int batchInactiveCount = 0;
				int batchActiveCount = 0;
				int batchNullCount = 0;
				for (Map<String, Object> balance : balances) {
					Object userIdObj = balance.get("userId");
					Object miningActiveObj = balance.get("miningActive");

					if (userIdObj == null) {
						log.warn("Skipping balance entry with null userId. Available keys: {}", balance.keySet());
						continue;
					}

					Long userId = null;
					if (userIdObj instanceof Number) {
						userId = ((Number) userIdObj).longValue();
					} else {
						try {
							userId = Long.parseLong(userIdObj.toString());
						} catch (NumberFormatException e) {
							log.warn("Invalid userId in balance: {}, type: {}", userIdObj, userIdObj.getClass().getName());
							continue;
						}
					}

					// Check if mining is inactive (miningActive is false)
					// IMPORTANT DEFINITIONS:
					// - miningActive = false: User HAS a mining session, but it's currently inactive/stopped
					//   → This is an "inactive" user (target of this endpoint)
					// - miningActive = null: User DOESN'T have a mining session (never started mining)
					//   → This is a "never started" user (excluded from this endpoint, use never-started endpoint)
					// - miningActive = true: User HAS a mining session AND it's currently active
					//   → This is an "active" user (excluded from this endpoint)
					// Note: miningActive can be Boolean, null, or potentially other types from JSON deserialization
					Boolean miningActive = null;
					if (miningActiveObj instanceof Boolean) {
						miningActive = (Boolean) miningActiveObj;
					} else if (miningActiveObj != null) {
						// Handle string "true"/"false" or other representations
						String miningActiveStr = miningActiveObj.toString().toLowerCase();
						if ("true".equals(miningActiveStr) || "1".equals(miningActiveStr)) {
							miningActive = true;
						} else if ("false".equals(miningActiveStr) || "0".equals(miningActiveStr) || miningActiveStr.isEmpty()) {
							miningActive = false;
						} else {
							log.warn("Unexpected miningActive value for userId {}: {} (type: {}), treating as false",
									userId, miningActiveObj, miningActiveObj.getClass().getName());
							miningActive = false;
						}
					} else {
						// null means user doesn't have a mining session (never started mining)
						// Skip: These users don't have a mining session, so they're not "inactive"
						// Use /push/never-started-users endpoint for these users instead
						batchNullCount++;
						if (batchNullCount <= 5) { // Log first 5 null cases for debugging
							log.debug("Skipping user who never started mining (no mining session, miningActive=null): userId={}", userId);
						}
						continue;
					}

					// Log first few inactive users found for debugging
					if (batchInactiveCount < 3 && Boolean.FALSE.equals(miningActive)) {
						log.info("Found inactive user: userId={}, miningActive={}, miningActiveObj={}, miningActiveObjType={} (has mining session but inactive)",
								userId, miningActive, miningActiveObj,
								miningActiveObj != null ? miningActiveObj.getClass().getName() : "null");
					}

					// Only send to users with inactive mining (miningActive is false, NOT null)
					// Criteria: User HAS a mining session (miningActive exists) but it's inactive (miningActive = false)
					// Users with miningActive = null don't have a mining session and should be handled by never-started endpoint
					if (Boolean.FALSE.equals(miningActive) || (miningActive == null && miningActiveObj != null && "false".equalsIgnoreCase(miningActiveObj.toString()))) {
						totalUsers.incrementAndGet();
						batchInactiveCount++;
						inactiveCount++;

						try {
							Map<String, Object> result = notificationServiceRestClient.sendPushNotification(
									userId,
									notificationTitle,
									notificationBody,
									dataPayload,
									priority
							);

							// Check if notification was actually sent to devices
							Object devicesSentObj = result.get("devicesSent");
							int devicesSent = devicesSentObj != null && devicesSentObj instanceof Number
									? ((Number) devicesSentObj).intValue()
									: 0;
							Object successObj = result.get("success");
							boolean success = successObj instanceof Boolean ? (Boolean) successObj : false;

							if (devicesSent > 0 && success) {
								successfulSends.incrementAndGet();
								log.debug("Successfully sent push notification to user with inactive mining: userId={}, miningActive={}, devicesSent={}",
										userId, miningActive, devicesSent);
							} else {
								failedSends.incrementAndGet();
								failedUserIds.add(userId);
								Object messageObj = result.get("message");
								String message = messageObj != null ? messageObj.toString() : "Unknown error";
								log.warn("Push notification not delivered to user with inactive mining: userId={}, miningActive={}, devicesSent={}, message={}",
										userId, miningActive, devicesSent, message);
							}
						} catch (Exception e) {
							failedSends.incrementAndGet();
							failedUserIds.add(userId);
							log.warn("Failed to send push notification to user with inactive mining: userId={}, miningActive={}, error={}",
									userId, miningActive, e.getMessage());
						}
					} else {
						log.debug("Skipping user with active mining: userId={}, miningActive={}", userId, miningActive);
					}
				}

				// Check if there are more pages
				if (balancesPage.isLast()) {
					hasMore = false;
				} else {
					currentPage++;
				}
			}

			Map<String, Object> result = new HashMap<>();
			result.put("totalUsers", totalUsers.get());
			result.put("successfulSends", successfulSends.get());
			result.put("failedSends", failedSends.get());
			result.put("failedUserIds", failedUserIds);
			result.put("message", String.format(
					"Push notification sent to %d out of %d users with inactive mining sessions",
					successfulSends.get(), totalUsers.get()));

			auditService.logAction(adminId, null,
					com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_CREATED,
					"NOTIFICATION", null,
					String.format("Sent push notification to %d users with inactive mining sessions: %s",
							successfulSends.get(), notificationTitle));

			// Note: Timestamp is already saved when the async job starts, not here

			log.info("Admin {}: Completed sending push notifications to users with inactive mining sessions: totalProcessed={}, inactiveUsersFound={}, totalSentTo={}, successful={}, failed={}, failedUserIds={}",
					adminId, totalProcessed, inactiveCount, totalUsers.get(), successfulSends.get(), failedSends.get(),
					failedUserIds.size() > 0 ? failedUserIds.subList(0, Math.min(10, failedUserIds.size())) : "none");

			// Method is void - no return needed (processing happens asynchronously)

		} catch (Exception e) {
			log.error("Admin {}: Error sending push notifications to users with inactive mining sessions", adminId, e);
			// Don't throw exception in async method - just log it
		}
	}

	/**
	 * Processes push notifications to never-started users asynchronously
	 */
	private void processPushNotificationsToNeverStartedUsers(
			Long adminId,
			String notificationTitle,
			String notificationBody,
			Map<String, String> dataPayload,
			String priority) {

		AtomicInteger totalUsers = new AtomicInteger(0);
		AtomicInteger successfulSends = new AtomicInteger(0);
		AtomicInteger failedSends = new AtomicInteger(0);
		List<Long> failedUserIds = new java.util.ArrayList<>();

		try {
			// Fetch AP balances in batches to find users who never started mining
			// Users who never started mining have no mining record (miningActive is null or false)
			int pageSize = 1000; // Process 1000 users at a time
			int currentPage = 0;
			boolean hasMore = true;

			while (hasMore) {
				Pageable pageable = PageRequest.of(currentPage, pageSize);
				Page<Map<String, Object>> balancesPage = miningServiceRestClient.listApBalances(pageable);

				if (balancesPage.isEmpty()) {
					hasMore = false;
					break;
				}

				List<Map<String, Object>> balances = balancesPage.getContent();
				log.info("Processing batch {}: {} AP balances for never-started users", currentPage + 1, balances.size());

				// Filter for users who never started mining (miningActive is null or false)
				// Note: This is a simplified check - ideally we'd verify they have no mining history
				for (Map<String, Object> balance : balances) {
					Object userIdObj = balance.get("userId");
					Object miningActiveObj = balance.get("miningActive");

					if (userIdObj == null) {
						log.warn("Skipping balance entry with null userId. Available keys: {}", balance.keySet());
						continue;
					}

					Long userId = null;
					if (userIdObj instanceof Number) {
						userId = ((Number) userIdObj).longValue();
					} else if (userIdObj instanceof String) {
						try {
							userId = Long.parseLong((String) userIdObj);
						} catch (NumberFormatException e) {
							log.warn("Invalid userId format: {}", userIdObj);
							continue;
						}
					} else {
						log.warn("Unexpected userId type: {} (value: {})", userIdObj.getClass().getName(), userIdObj);
						continue;
					}

					// Check if user never started mining (miningActive is null)
					// IMPORTANT: miningActive = null means user DOESN'T have a mining session (never started)
					// This is different from inactive users who HAVE a session but it's inactive (miningActive = false)
					Boolean miningActive = null;
					if (miningActiveObj instanceof Boolean) {
						miningActive = (Boolean) miningActiveObj;
					} else if (miningActiveObj != null) {
						String miningActiveStr = miningActiveObj.toString().toLowerCase();
						if ("true".equals(miningActiveStr) || "1".equals(miningActiveStr)) {
							miningActive = true;
						} else if ("false".equals(miningActiveStr) || "0".equals(miningActiveStr) || miningActiveStr.isEmpty()) {
							miningActive = false;
						} else {
							log.warn("Unexpected miningActive value for userId {}: {} (type: {}), treating as never started",
									userId, miningActiveObj, miningActiveObj.getClass().getName());
							miningActive = null; // null means never started
						}
					} else {
						// null means no mining active status, treat as never started
						miningActive = null;
					}

					// Only send to users who never started mining (miningActive is null)
					// Criteria: User DOESN'T have a mining session (miningActive = null)
					// Note: Users with miningActive=false have a session but it's inactive (use inactive endpoint for those)
					// IMPORTANT: Only include users with miningActive = null (never started)
					// Do NOT include users with miningActive = false (those are inactive, not never-started)
					if (miningActive == null) {
						totalUsers.incrementAndGet();

						try {
							Map<String, Object> result = notificationServiceRestClient.sendPushNotification(
									userId,
									notificationTitle,
									notificationBody,
									dataPayload,
									priority
							);

							// Check if notification was actually sent to devices
							Object devicesSentObj = result.get("devicesSent");
							int devicesSent = devicesSentObj != null && devicesSentObj instanceof Number
									? ((Number) devicesSentObj).intValue()
									: 0;
							Object successObj = result.get("success");
							boolean success = successObj instanceof Boolean ? (Boolean) successObj : false;

							if (devicesSent > 0 && success) {
								successfulSends.incrementAndGet();
								log.debug("Successfully sent push notification to user who never started mining: userId={}, miningActive={}, devicesSent={}",
										userId, miningActive, devicesSent);
							} else {
								failedSends.incrementAndGet();
								failedUserIds.add(userId);
								Object messageObj = result.get("message");
								String message = messageObj != null ? messageObj.toString() : "Unknown error";
								log.warn("Push notification not delivered to user who never started mining: userId={}, miningActive={}, devicesSent={}, message={}",
										userId, miningActive, devicesSent, message);
							}
						} catch (Exception e) {
							failedSends.incrementAndGet();
							failedUserIds.add(userId);
							log.warn("Failed to send push notification to user who never started mining: userId={}, miningActive={}, error={}",
									userId, miningActive, e.getMessage());
						}
					} else {
						log.debug("Skipping user with active mining: userId={}, miningActive={}", userId, miningActive);
					}
				}

				// Check if there are more pages
				if (balancesPage.isLast()) {
					hasMore = false;
				} else {
					currentPage++;
				}
			}

			Map<String, Object> result = new HashMap<>();
			result.put("totalUsers", totalUsers.get());
			result.put("successfulSends", successfulSends.get());
			result.put("failedSends", failedSends.get());
			result.put("failedUserIds", failedUserIds);
			result.put("message", String.format(
					"Push notification sent to %d out of %d users who never started mining",
					successfulSends.get(), totalUsers.get()));

			auditService.logAction(adminId, null,
					com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_CREATED,
					"NOTIFICATION", null,
					String.format("Sent push notification to %d users who never started mining: %s",
							successfulSends.get(), notificationTitle));

			log.info("Admin {}: Completed sending push notifications to users who never started mining: total={}, successful={}, failed={}",
					adminId, totalUsers.get(), successfulSends.get(), failedSends.get());

			// Method is void - no return needed (processing happens asynchronously)

		} catch (Exception e) {
			log.error("Admin {}: Error sending push notifications to users who never started mining", adminId, e);
			// Don't throw exception in async method - just log it
		}
	}

	/**
	 * Translates English text to Russian for notification messages.
	 * Provides proper Russian translations for default mining-related messages.
	 *
	 * @param englishText The English text to translate
	 * @return Russian translation of the text (never returns empty string for non-empty input)
	 */
	private String translateToRussian(String englishText) {
		if (englishText == null || englishText.trim().isEmpty()) {
			return "";
		}

		String text = englishText.trim();
		String lowerText = text.toLowerCase();

		// Template title translations
		if (lowerText.equals("come back to mining!") || lowerText.equals("come back to mining")) {
			return "Вернитесь к майнингу!";
		}
		if (lowerText.equals("start your mining journey!") || lowerText.equals("start your mining journey")) {
			return "Начните свое путешествие в майнинге!";
		}
		if (lowerText.equals("start mining now")) {
			return "Начните майнинг сейчас";
		}

		// Template body translations for inactive users
		if (lowerText.contains("we noticed you haven't been mining") ||
				lowerText.contains("we noticed you haven't been mining lately")) {
			return "Мы заметили, что вы давно не занимались майнингом. Начните майнинг снова, чтобы заработать AP Credits и разблокировать удивительные награды!";
		}

		// Default body translations for inactive users
		if (lowerText.contains("you currently didn't mining") ||
				lowerText.contains("you currently don't mining") ||
				lowerText.contains("you are not mining")) {
			if (lowerText.contains("please start mining and earn ap")) {
				return "Вы сейчас не майните, пожалуйста, начните майнинг и зарабатывайте AP.";
			}
			return "Вы сейчас не майните, пожалуйста, начните майнинг и зарабатывайте AP.";
		}

		// Template body translations for never started users
		if (lowerText.contains("welcome! start mining now to earn ap credits")) {
			return "Добро пожаловать! Начните майнинг прямо сейчас, чтобы заработать AP Credits. Это легко, весело и выгодно. Начните свое путешествие сегодня!";
		}

		// Default body translations for never started users
		if (lowerText.contains("you haven't started mining") ||
				lowerText.contains("you haven't started")) {
			if (lowerText.contains("start mining and earn ap")) {
				return "Вы еще не начинали майнинг. Начните майнинг и зарабатывайте AP!";
			}
			return "Вы еще не начинали майнинг. Начните майнинг и зарабатывайте AP!";
		}

		// Common phrase translations
		String translated = text
				.replace("Start Mining Now", "Начните майнинг сейчас")
				.replace("start mining now", "начните майнинг сейчас")
				.replace("Start Mining", "Начните майнинг")
				.replace("start mining", "начните майнинг")
				.replace("Mining", "Майнинг")
				.replace("mining", "майнинг")
				.replace("earn AP", "зарабатывайте AP")
				.replace("Earn AP", "Зарабатывайте AP")
				.replace("earn AP Credits", "зарабатывайте AP кредиты")
				.replace("Earn AP Credits", "Зарабатывайте AP кредиты")
				.replace("please", "пожалуйста")
				.replace("Please", "Пожалуйста")
				.replace("You currently", "Вы сейчас")
				.replace("you currently", "вы сейчас")
				.replace("You haven't", "Вы еще не")
				.replace("you haven't", "вы еще не")
				.replace("started", "начинали")
				.replace("Started", "Начинали");

		// If translation was applied, return it
		if (!translated.equals(text)) {
			return translated;
		}

		// For custom text that doesn't match patterns, log a warning
		// In production, consider calling notification service translation API
		log.warn("Custom text provided without explicit Russian translation: {}. Using English text. Consider adding translation API integration.", text);
		return text; // Return original for now - admin should provide Russian translation for custom text
	}
}
