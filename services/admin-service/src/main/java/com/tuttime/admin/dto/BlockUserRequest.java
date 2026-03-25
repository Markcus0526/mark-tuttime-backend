package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Block User Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockUserRequest {
	@NotBlank(message = "Reason is required")
	@Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
	private String reason;
}

