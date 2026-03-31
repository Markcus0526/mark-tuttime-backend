package com.tuttime.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pos_sync_queues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosSyncQueue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pos_log_id", nullable = false)
	private PosLog posLog;

	@Column(name = "next_retry_at", nullable = false)
	private Instant nextRetryAt;

	@Builder.Default
	@Column(name = "retry_count", nullable = false)
	private Integer retryCount = 0;

	@Column(name = "status", nullable = false, length = 20)
	private String status; // PENDING, COMPLETED, FAILED

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
