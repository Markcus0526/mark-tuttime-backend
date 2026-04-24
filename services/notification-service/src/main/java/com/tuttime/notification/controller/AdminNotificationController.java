package com.tuttime.notification.controller;

import com.tuttime.common.dto.ApiRes; // Assuming common location
import com.tuttime.notification.dto.AdminNotificationDto;
import com.tuttime.notification.entity.NotificationTemplate;
import com.tuttime.notification.service.AdminNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/admin")
@RequiredArgsConstructor
@Tag(name = "1.Admin Notification Service", description = "Management of system-wide message templates")
public class AdminNotificationController {

	private final AdminNotificationService adminNotificationService;

	@Operation(summary = "Create a new notification template", description = "Defines a new template for a specific key and channel.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Template created"),
			@ApiResponse(responseCode = "409", description = "Configuration already exists")
	})
	@PostMapping
	public ResponseEntity<ApiRes<NotificationTemplate>> create(
			@Valid @RequestBody AdminNotificationDto.TemplateRequest request) {

		NotificationTemplate template = adminNotificationService.createTemplate(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiRes.success("Template created successfully", template));
	}

	@Operation(summary = "Update template content", description = "Updates the title, body, or active status of an existing template.")
	@PutMapping("/{templateId}")
	public ResponseEntity<ApiRes<NotificationTemplate>> update(
			@PathVariable Integer templateId,
			@Valid @RequestBody AdminNotificationDto.TemplateRequest request) {

		NotificationTemplate updated = adminNotificationService.updateTemplate(templateId, request);
		return ResponseEntity.ok(ApiRes.success("Template updated successfully", updated));
	}

	@Operation(summary = "Preview template rendering", description = "Accepts raw content and params to show the rendered result.")
	@PostMapping("/preview")
	public ResponseEntity<ApiRes<String>> preview(
			@Valid @RequestBody AdminNotificationDto.PreviewRequest request) {

		String rendered = adminNotificationService.renderPreview(request.content(), request.mockParams());
		return ResponseEntity.ok(ApiRes.success("Preview rendered successfully", rendered));
	}

	@Operation(summary = "Get all templates", description = "Retrieves a list of all defined notification templates.")
	@GetMapping
	public ResponseEntity<ApiRes<List<NotificationTemplate>>> list() {
		return ResponseEntity.ok(ApiRes.success("Templates retrieved", adminNotificationService.getAllTemplates()));
	}

}
