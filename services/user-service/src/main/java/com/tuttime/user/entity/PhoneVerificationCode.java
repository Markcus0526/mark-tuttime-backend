package com.tuttime.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "phone_verification_codes",
		indexes = {
				@Index(name = "idx_phone_codes_phone_number", columnList = "phone_number")
		})
@Data
public class PhoneVerificationCode {
	@Id
	@GeneratedValue(generator = "UUID")
	@Column(name = "id", nullable = false, length = 36)
	private UUID id;

	@Column(name = "phone_number", length = 20, nullable = false)
	private String phoneNumber;

	@Column(name = "code", length = 10, nullable = false)
	private String code;

	@Column(name = "expires_at", nullable = false)
	private OffsetDateTime expiresAt;

	@Column(name = "used", nullable = false)
	private boolean used = false;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();
}
