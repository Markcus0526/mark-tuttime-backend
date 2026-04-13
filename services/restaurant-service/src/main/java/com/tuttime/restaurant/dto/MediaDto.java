package com.tuttime.restaurant.dto;

import com.tuttime.restaurant.entity.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class MediaDto {

	@Schema(description = "Branch request details")
	public record MediaRequest(
		@NotNull(message = "Entity ID is required")
		UUID entityId,

		@NotBlank(message = "Media URL is required")
		@Size(max = 1024, message = "Media URL must be at most 1024 characters")
		String url,

		@Size(max = 255, message = "Alt text must be at most 255 characters")
		String altText,

		Boolean primaryEntity,

		@NotNull(message = "Media type is required")
		MediaType mediaType // Could be validated against Enum in service layer
	) {
	}

	@Schema(description = "Branch response details")
	public record MediaResponse(

		UUID mediaId,
		UUID entityId,
		String url,
		String altText,
		boolean primaryEntity,
		String mediaType, // IMAGE, VIDEO
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
	) {
	}

}
