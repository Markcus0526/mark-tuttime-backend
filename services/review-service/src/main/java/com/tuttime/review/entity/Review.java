package com.tuttime.review.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.common.entity.BaseUuidEntity;
import com.tuttime.review.entity.enums.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews", schema = "review_service")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Schema(description = "Review Entity representing user feedback for a branch")
public class Review extends BaseUuidEntity {

	@Column(name = "user_id", nullable = false)
	@Schema(description = "ID of the user who wrote the review", requiredMode = Schema.RequiredMode.REQUIRED)
	private UUID userId;

	@Column(name = "branch_id", nullable = false)
	@Schema(description = "ID of the branch being reviewed")
	private UUID branchId;

	@Min(1) @Max(5)
	@Column(nullable = false)
	@Schema(description = "Rating from 1 to 5", example = "5")
	private Integer rating;

	@Column(columnDefinition = "TEXT")
	@Schema(description = "User's textual comment", example = "Great food and atmosphere!")
	private String comment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Builder.Default
	@Schema(description = "Moderation status", allowableValues = {"PENDING", "APPROVED", "REJECTED", "DELETED"})
	private ReviewStatus status = ReviewStatus.PENDING;

	@Version
	@Schema(description = "Optimistic locking version")
	private Long version;
}

