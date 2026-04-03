package com.tuttime.review.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_moderation_logs", schema = "review_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewModerationLog {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@Column(name = "moderator_id", nullable = false)
	private UUID moderatorId;

	@Column(nullable = false, length = 50)
	private String action;

	@Column(columnDefinition = "TEXT")
	private String reason;

	@Column(name = "created_at", nullable = false, updatable = false)
	@Builder.Default
	private OffsetDateTime createdAt = OffsetDateTime.now();
}