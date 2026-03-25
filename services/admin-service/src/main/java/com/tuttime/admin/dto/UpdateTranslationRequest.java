package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating template translation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTranslationRequest {

	@NotBlank(message = "Translated title is required")
	private String translatedTitle;

	@NotBlank(message = "Translated message is required")
	private String translatedMessage;
}

