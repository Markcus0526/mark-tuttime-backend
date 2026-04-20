package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseSerialEntity;
import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.pos.entity.enums.PosProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "pos_system_configs", schema = "pos_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PosSystemConfig extends BaseSerialEntity {

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class) // Maps Java Enum to Postgres TYPE pos_provider
	@Column(name = "pos_provider", unique = true, nullable = false)
	private PosProvider posProvider;

	@Column(name = "api_endpoint", nullable = false)
	private String apiEndpoint;

	@Column(name = "webhook_secret", columnDefinition = "TEXT")
	private String webhookSecret;

	@Column(nullable = false)
	@Builder.Default
	private Boolean enabled = true;
}