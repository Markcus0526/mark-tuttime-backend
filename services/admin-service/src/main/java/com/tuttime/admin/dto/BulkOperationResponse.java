package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk Operation Response DTO
 *
 * <p>Contains results of bulk operations including success/failure counts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResponse {

	private int totalRequested;
	private int successful;
	private int failed;
	private List<BulkOperationResult> results;
	private String message;

	/**
	 * Individual operation result
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class BulkOperationResult {
		private Long userId;
		private boolean success;
		private String errorMessage;
	}

	/**
	 * Create success response
	 */
	public static BulkOperationResponse success(int total, int successful, List<BulkOperationResult> results) {
		return BulkOperationResponse.builder()
				.totalRequested(total)
				.successful(successful)
				.failed(total - successful)
				.results(results)
				.message(String.format("Successfully processed %d of %d users", successful, total))
				.build();
	}

	/**
	 * Create failure response
	 */
	public static BulkOperationResponse failure(int total, String errorMessage) {
		return BulkOperationResponse.builder()
				.totalRequested(total)
				.successful(0)
				.failed(total)
				.message("Bulk operation failed: " + errorMessage)
				.build();
	}
}

