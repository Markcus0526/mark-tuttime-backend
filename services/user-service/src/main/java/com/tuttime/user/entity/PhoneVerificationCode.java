package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "phone_verification_codes",
		indexes = {
				@Index(name = "idx_phone_codes_phone_number", columnList = "phone_number")
		})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PhoneVerificationCode extends BaseTimeStampEntity {
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
	@Builder.Default
	private boolean used = false;
}
