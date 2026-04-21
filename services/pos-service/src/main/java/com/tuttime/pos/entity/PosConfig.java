package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseSerialEntity;
import com.tuttime.pos.entity.enums.PosProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "pos_configs", schema = "pos_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PosConfig extends BaseSerialEntity {

	@Column(name = "branch_id", nullable = false, unique = true)
	private UUID branchId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "pos_provider", nullable = false)
	private PosProvider posProvider;

	@Column(name = "api_endpoint", nullable = false)
	private String apiEndpoint;

	@Column(name = "api_key")
	private String apiKey;

	@Column(name = "webhook_secret")
	private String webhookSecret;

	@Column(name = "organization_id")
	private String organizationId;

	@Column(name = "terminal_group_id")
	private String terminalGroupId;

	@Builder.Default
	private boolean enabled = true;
}