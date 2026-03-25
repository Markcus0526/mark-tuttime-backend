package com.tuttime.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens", schema = "user_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {
	@Id
	@GeneratedValue(generator = "gen_random_uuid")
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, unique = true)
	private String token;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Builder.Default
	@Column(nullable = false)
	private boolean used = false;
}
