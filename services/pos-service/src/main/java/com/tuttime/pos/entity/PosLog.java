package com.tuttime.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pos_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pos_system_id", nullable = false)
	private PosSystemConfig posSystemConfig;

	@Column(name = "entity_type", nullable = false, length = 50)
	private String entityType; // e.g., BOOKING, ORDER, MENU

	@Column(name = "entity_id", nullable = false, length = 100)
	private String entityId; // Backend system entity ID

	@Column(name = "status", nullable = false, length = 20)
	private String status; // SUCCESS, FAILED, PENDING

	@Builder.Default
	@Column(name = "attempt_count", nullable = false)
	private Integer attemptCount = 0;

	@Column(name = "last_attempt")
	private Instant lastAttempt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = updatedAt = Instant.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}
}