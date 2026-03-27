package com.tuttime.restaurant.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

// --- MediaRequest DTO ---
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaRequest {

	@NotBlank(message = "Entity type must not be blank")
	@Pattern(regexp = "RESTAURANT|BRANCH|ZONE|TABLE", message = "Entity type must be one of RESTAURANT, BRANCH, ZONE, TABLE")
	private String entityType;

	@NotNull(message = "Entity ID must not be null")
	private UUID entityId;

	@NotBlank(message = "URL must not be blank")
	@Size(max = 1024, message = "URL must be at most 1024 characters")
	@Pattern(regexp = "^(https?://).+", message = "URL must be a valid HTTP/HTTPS URL")
	private String url;

	@Size(max = 255, message = "Alt text must be at most 255 characters")
	private String altText;

	@Builder.Default
	private Boolean isPrimary = false;

	@Builder.Default
	@Pattern(regexp = "IMAGE|VIDEO", message = "Media type must be IMAGE or VIDEO")
	private String mediaType = "IMAGE";

	// Getters and setters omitted for brevity
}
