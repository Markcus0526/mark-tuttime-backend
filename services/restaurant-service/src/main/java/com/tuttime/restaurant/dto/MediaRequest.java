package com.tuttime.restaurant.dto;

import com.tuttime.restaurant.enums.MediaType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaRequest {

	@NotNull(message = "Entity type is required")
	private String entityType; // Could be validated against Enum in service layer

	@NotNull(message = "Entity ID is required")
	private UUID entityId;

	@NotBlank(message = "Media URL is required")
	@Size(max = 1024, message = "Media URL must be at most 1024 characters")
	private String url;

	@Size(max = 255, message = "Alt text must be at most 255 characters")
	private String altText;

	private Boolean isPrimary;

	@NotNull(message = "Media type is required")
	private MediaType mediaType; // Could be validated against Enum in service layer
}