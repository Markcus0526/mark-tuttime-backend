package com.tuttime.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens",
		indexes = {
				@Index(name = "idx_password_reset_tokens_user_id", columnList = "user_id")
		})
@Data
public class PasswordResetToken {
	@Id
	@Column(columnDefinition = "uuid", updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@JoinColumn(name = "user_id", nullable = false)
	private UUID userId;

	@Column(length = 255, unique = true, nullable = false)
	private String token;

	@Column(nullable = false)
	private OffsetDateTime expiresAt;

	@Column(nullable = false)
	private boolean used = false;

	@Column(nullable = false, updatable = false)
	@CreationTimestamp
	private OffsetDateTime createdAt;
}
