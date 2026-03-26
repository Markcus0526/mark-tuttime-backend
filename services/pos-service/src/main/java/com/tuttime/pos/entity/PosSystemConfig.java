package com.tuttime.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pos_system_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosSystemConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "pos_system", unique = true, nullable = false, length = 50)
	private String posSystem;

	@Column(name = "api_endpoint", nullable = false, columnDefinition = "TEXT")
	private String apiEndpoint;

	@Column(name = "webhook_secret", columnDefinition = "TEXT")
	private String webhookSecret;

	@Builder.Default
	@Column(name = "enabled", nullable = false)
	private Boolean enabled = true;

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