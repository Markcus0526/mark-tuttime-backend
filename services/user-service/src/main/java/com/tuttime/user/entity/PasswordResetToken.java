package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens",
		indexes = {
				@Index(name = "idx_password_reset_tokens_user_id", columnList = "user_id")
		})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PasswordResetToken extends BaseTimeStampEntity {
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
	@Builder.Default
	private boolean used = false;
}
