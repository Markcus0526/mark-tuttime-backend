package com.tuttime.admin.controller;

import com.tuttime.admin.dto.CreateTemplateRequest;
import com.tuttime.admin.dto.UpdateTemplateRequest;
import com.tuttime.admin.dto.UpdateTranslationRequest;
import com.tuttime.admin.kafka.TemplateEventProducer;
import com.tuttime.admin.rest.client.NotificationServiceRestClient;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller for Notification Template Management
 * Handles template CRUD operations and publishes events to Kafka
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notifications/templates")
@Tag(name = "Admin Notification Template Management", description = "Admin APIs for notification template management")
@RequiredArgsConstructor
public class AdminNotificationTemplateController {

	private final NotificationServiceRestClient notificationServiceRestClient;
	private final AdminAuditService auditService;
	private final TemplateEventProducer templateEventProducer; // Optional - only if Kafka is enabled

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List Templates", description = "Get list of notification templates with optional category filter")
	public ResponseEntity<ApiResponse<Map<String, Object>>> listTemplates(
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) String category,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Listing templates, category filter: {}", adminId, category);

		Map<String, Object> pageData = notificationServiceRestClient.listTemplates(pageable, category);

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_VIEWED,
				"TEMPLATE", null, "Listed notification templates");

		return ResponseEntity.ok(ApiResponse.success("Templates retrieved successfully", pageData));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get Template", description = "Get template details")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getTemplate(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Getting template: {}", adminId, id);

		Map<String, Object> template = notificationServiceRestClient.getTemplate(id);

		return ResponseEntity.ok(ApiResponse.success("Template retrieved successfully", template));
	}

	@GetMapping("/key/{key}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get Template by Key", description = "Get template by template key")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getTemplateByKey(
			@PathVariable String key,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Getting template by key: {}", adminId, key);

		Map<String, Object> template = notificationServiceRestClient.getTemplateByKey(key);

		return ResponseEntity.ok(ApiResponse.success("Template retrieved successfully", template));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Create Template", description = "Create a new notification template")
	public ResponseEntity<ApiResponse<Map<String, Object>>> createTemplate(
			@Valid @RequestBody CreateTemplateRequest request,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Creating template: templateKey={}, category={}",
				adminId, request.getTemplateKey(), request.getCategory());

		// Save template to notification-service DB (via REST)
		Map<String, Object> templateRequest = new HashMap<>();
		templateRequest.put("templateKey", request.getTemplateKey());
		templateRequest.put("category", request.getCategory());
		templateRequest.put("notificationType", request.getNotificationType());
		templateRequest.put("sourceLanguage", request.getSourceLanguage() != null ? request.getSourceLanguage() : "en");
		templateRequest.put("sourceTitle", request.getSourceTitle());
		templateRequest.put("sourceMessage", request.getSourceMessage());
		templateRequest.put("variables", request.getVariables());
		templateRequest.put("isSystem", request.getIsSystem() != null ? request.getIsSystem() : false);

		Map<String, Object> template = notificationServiceRestClient.createTemplate(templateRequest);

		Long templateId = null;
		if (template.get("templateId") != null) {
			Object idObj = template.get("templateId");
			if (idObj instanceof Number) {
				templateId = ((Number) idObj).longValue();
			}
		}

		// Publish to Kafka for auto-translation
		if (templateEventProducer != null && templateId != null) {
			templateEventProducer.sendTemplateCreated(
					templateId,
					request.getTemplateKey(),
					request.getCategory(),
					request.getNotificationType(),
					request.getSourceLanguage() != null ? request.getSourceLanguage() : "en",
					request.getSourceTitle(),
					request.getSourceMessage(),
					request.getVariables(),
					request.getIsSystem() != null ? request.getIsSystem() : false,
					adminId
			);
			log.info("Template created event published to Kafka: templateId={}", templateId);
		}

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_CREATED,
				"TEMPLATE", templateId,
				"Created template: " + request.getTemplateKey());

		return ResponseEntity.ok(ApiResponse.success("Template created successfully", template));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update Template", description = "Update notification template")
	public ResponseEntity<ApiResponse<Map<String, Object>>> updateTemplate(
			@PathVariable Long id,
			@Valid @RequestBody UpdateTemplateRequest request,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Updating template: {}", adminId, id);

		Map<String, Object> templateRequest = new HashMap<>();
		if (request.getSourceTitle() != null) {
			templateRequest.put("sourceTitle", request.getSourceTitle());
		}
		if (request.getSourceMessage() != null) {
			templateRequest.put("sourceMessage", request.getSourceMessage());
		}
		if (request.getVariables() != null) {
			templateRequest.put("variables", request.getVariables());
		}
		if (request.getIsActive() != null) {
			templateRequest.put("isActive", request.getIsActive());
		}

		Map<String, Object> template = notificationServiceRestClient.updateTemplate(id, templateRequest);

		// Publish to Kafka for re-translation if content changed
		if (templateEventProducer != null &&
				(request.getSourceTitle() != null || request.getSourceMessage() != null)) {

			// Get template key from response
			String templateKey = (String) template.get("templateKey");

			templateEventProducer.sendTemplateUpdated(
					id,
					templateKey,
					request.getSourceTitle(),
					request.getSourceMessage(),
					request.getVariables(),
					request.getIsActive(),
					adminId
			);
			log.info("Template updated event published to Kafka: templateId={}", id);
		}

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_UPDATED,
				"TEMPLATE", id,
				"Updated template: " + id);

		return ResponseEntity.ok(ApiResponse.success("Template updated successfully", template));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Delete Template", description = "Delete notification template (system templates cannot be deleted)")
	public ResponseEntity<ApiResponse<Void>> deleteTemplate(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Deleting template: {}", adminId, id);

		notificationServiceRestClient.deleteTemplate(id);

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_DELETED,
				"TEMPLATE", id,
				"Deleted template: " + id);

		return ResponseEntity.ok(ApiResponse.success("Template deleted successfully", null));
	}

	@GetMapping("/{id}/translations")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get Template Translations", description = "Get all translations for a template")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTemplateTranslations(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Getting translations for template: {}", adminId, id);

		List<Map<String, Object>> translations = notificationServiceRestClient.getTemplateTranslations(id);

		return ResponseEntity.ok(ApiResponse.success("Translations retrieved successfully", translations));
	}

	@PutMapping("/{id}/translations/{languageCode}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update Translation", description = "Update translation for a specific language")
	public ResponseEntity<ApiResponse<Map<String, Object>>> updateTranslation(
			@PathVariable Long id,
			@PathVariable String languageCode,
			@Valid @RequestBody UpdateTranslationRequest request,
			@AuthenticationPrincipal Long adminId) {

		log.info("Admin {}: Updating translation: templateId={}, language={}", adminId, id, languageCode);

		Map<String, Object> translationRequest = new HashMap<>();
		translationRequest.put("translatedTitle", request.getTranslatedTitle());
		translationRequest.put("translatedMessage", request.getTranslatedMessage());

		Map<String, Object> translation = notificationServiceRestClient.updateTranslation(
				id, languageCode, translationRequest);

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_UPDATED,
				"TEMPLATE_TRANSLATION", id,
				"Updated translation: " + languageCode);

		return ResponseEntity.ok(ApiResponse.success("Translation updated successfully", translation));
	}
}
