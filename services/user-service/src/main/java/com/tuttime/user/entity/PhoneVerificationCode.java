package com.tuttime.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phone_verification_codes", schema = "user_service",
		indexes = @Index(name = "idx_active_phone_codes", columnList = "phone_number", unique = false))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneVerificationCode {
	@Id
	@GeneratedValue(generator = "gen_random_uuid")
	private UUID id;

	@Column(name = "phone_number", nullable = false)
	private String phoneNumber;

	@Column(name = "code", nullable = false)
	private String code;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Builder.Default
	@Column(nullable = false)
	private boolean used = false;

	@Builder.Default
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();
}
