package com.tuttime.notification.dto;

import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.notification.entity.enums.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class AdminNotificationDto {

	@Schema(description = "DTO for creating or updating a notification template")
	public record TemplateRequest(
			@Schema(description = "Unique key for the event", example = "BOOKING_CREATED")
			@NotBlank(message = "Template key is required")
			NotificationTemplateKey templateKey,

			@Schema(description = "The communication channel", example = "EMAIL")
			@NotNull(message = "Channel is required")
			NotificationChannel channel,

			@Schema(description = "ISO language code", example = "en")
			@NotBlank(message = "Language is required")
			String language,

			@Schema(description = "Subject line or notification title", example = "Your Table is Ready!")
			@NotBlank(message = "Title is required")
			String title,

			@Schema(description = "Body content with {{placeholders}}", example = "Hello {{userName}}, see you at {{time}}.")
			@NotBlank(message = "Content is required")
			String content,

			@Schema(description = "Status of the template", example = "true")
			boolean active
	) {}

	@Schema(description = "DTO for previewing a template before saving")
	public record PreviewRequest(
			@Schema(description = "Raw content to render")
			@NotBlank(message = "Content is required")
			String content,

			@Schema(description = "Mock data to inject into placeholders", example = "{\"userName\": \"John Doe\"}")
			Map<String, Object> mockParams
	) {}
}
