package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk Operation Request DTO
 *
 * <p>Generic DTO for bulk operations on multiple users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationRequest {

	@NotEmpty(message = "User IDs list cannot be empty")
	@Size(min = 1, max = 1000, message = "Can process 1-1000 users at a time")
	@NotNull(message = "User IDs are required")
	private List<Long> userIds;

	@Size(max = 500, message = "Reason must not exceed 500 characters")
	private String reason;
}

