package com.tuttime.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pos_error_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosErrorDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pos_log_id", nullable = false)
	private PosLog posLog;

	@Column(name = "error_code", length = 100)
	private String errorCode;

	@Column(name = "error_stacktrace", columnDefinition = "TEXT")
	private String errorStacktrace;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = Instant.now();
	}
}
