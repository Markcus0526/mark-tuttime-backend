package com.tuttime.review.dto;

import com.tuttime.review.entity.enums.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReviewDto {

	@Schema(description = "Request body for creating a new review")
	public record ReviewRequest(
			@NotNull @Schema(description = "Target branch ID")
			UUID branchId,

			@Min(1) @Max(5)
			@Schema(description = "Rating score", example = "5")
			int rating,

			@Size(max = 1000)
			@Schema(description = "Optional review text")
			String comment
	) {
	}

	@Schema(description = "Request object for review moderation actions")
	public record ReviewModerationRequest(

			@NotNull(message = "New status is required")
			@Schema(description = "The target status for the review", example = "APPROVED",
					allowableValues = {"APPROVED", "REJECTED"})
			ReviewStatus status,

			@NotNull(message = "Action description is required")
			@Size(max = 50)
			@Schema(description = "Short action code for logging", example = "APPROVE_CONTENT")
			String action,

			@Size(max = 500)
			@Schema(description = "Optional reason for rejection or moderation notes",
					example = "Review contains inappropriate language.")
			String reason
	) {
	}

	@Schema(description = "Review output payload")
	public record ReviewResponse(
			@Schema(example = "uuid")
			UUID reviewId,

			@Schema(description = "Basic user info enriched via gRPC")
			UserDetailDto user, // Nested DTO instead of just userId

			@Schema(example = "5")
			int rating,

			@Schema(example = "Great food!")
			String comment,

			@Schema(example = "APPROVED")
			ReviewStatus status,

			@Schema(description = "ISO-8601 Timestamp")
			OffsetDateTime createdAt
	) {

		@Builder
		public record UserDetailDto(
				UUID userId,
				String phoneNumber,
				String email,
				String fullName,
				String profilePicture
		) {
		}
	}
}
