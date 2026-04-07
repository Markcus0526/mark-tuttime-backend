package com.tuttime.review.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_moderation_logs", schema = "review_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewModerationLog extends BaseTimeStampEntity {
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
}