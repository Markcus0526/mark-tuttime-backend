package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk AP Balance Adjustment Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkApAdjustmentRequest {

	@NotEmpty(message = "User IDs list cannot be empty")
	@Size(min = 1, max = 1000, message = "Can process 1-1000 users at a time")
	@NotNull(message = "User IDs are required")
	private List<Long> userIds;

	@NotNull(message = "Amount is required")
	private Long amount;

	@NotNull(message = "Type is required")
	private AdjustmentType type;

	@NotBlank(message = "Reason is required")
	@Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
	private String reason;

	public enum AdjustmentType {
		CREDIT, DEBIT
	}
}

