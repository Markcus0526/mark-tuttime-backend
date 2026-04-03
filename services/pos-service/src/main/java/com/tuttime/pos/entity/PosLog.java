package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pos_logs", schema = "pos_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosLog extends BaseTimeStampEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pos_system_id", nullable = false)
	private PosSystemConfig config;

	@Column(name = "entity_type", nullable = false)
	private String entityType;

	@Column(name = "entity_id", nullable = false, length = 100)
	private UUID entityId;

	@Column(nullable = false)
	private String status; // Uses values from pos_status enum

	@Column(name = "attempt_count")
	@Builder.Default
	private Integer attemptCount = 0;

	@Column(name = "last_attempt")
	private OffsetDateTime lastAttempt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "request_payload")
	private Map<String, Object> requestPayload;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "response_payload")
	private Map<String, Object> responsePayload;
}
