package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.user.entity.enums.VerificationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_codes")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class VerificationCode extends BaseTimeStampEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String target; // email or phone

	@Column(name = "code_hash", nullable = false)
	private String codeHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private VerificationType type;

	@Column(nullable = false)
	private OffsetDateTime expiresAt;

	@Builder.Default
	private boolean used = false;
}

