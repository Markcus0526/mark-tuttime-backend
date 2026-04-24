package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import com.tuttime.common.enums.VerificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Entity
@Table(name = "verification_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class VerificationCode extends BaseUuidEntity {

	@Column(nullable = false)
	private String target; // email or phone

	@Column(name = "code_hash", nullable = false)
	private String codeHash;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(nullable = false, columnDefinition = "verification_type")
	private VerificationType type;

	@Column(nullable = false)
	private OffsetDateTime expiresAt;

	@Builder.Default
	private boolean used = false;
}

