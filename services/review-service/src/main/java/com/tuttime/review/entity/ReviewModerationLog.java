package com.tuttime.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_moderation_logs",
		indexes = {
				@Index(name = "idx_moderation_review_id", columnList = "review_id"),
				@Index(name = "idx_moderation_moderator_id", columnList = "moderator_id")
		})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewModerationLog {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name="UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "review_id", nullable = false)
	private UUID reviewId;  // Reference to Review

	@Column(name = "moderator_id")
	private UUID moderatorId;  // Reference to User who moderated, nullable

	@Column(nullable = false, length = 50)
	private String action;

	@Column(columnDefinition = "TEXT")
	private String reason;

	@Builder.Default
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@PrePersist
	protected void onCreate() {
		createdAt = Instant.now();
	}
}
