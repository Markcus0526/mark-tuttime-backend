package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.user.entity.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_auth_methods")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserAuthMethod extends BaseTimeStampEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false, length = 36)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "auth_type", nullable = false)
	private AuthProvider authType;

	@Column(name = "credential", columnDefinition = "TEXT")
	private String credential;

	@Column(name = "verified", nullable = false)
	@Builder.Default
	private boolean verified = false;
}

