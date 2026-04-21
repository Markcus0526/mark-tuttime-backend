package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseSerialEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pos_logs", schema = "pos_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PosLog extends BaseSerialEntity {

	@Column(name = "branch_id", nullable = false)
	private UUID branchId;

	@Column(name = "entity_id", nullable = false)
	private UUID entityId; // Can be bookingId or orderId

	@Column(name = "entity_type", nullable = false)
	private String entityType; // "BOOKING", "ORDER", "MENU"

	@Column(nullable = false)
	private String action; // "RESERVE_TABLE", "SEND_ORDER"

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "request_payload")
	private Map<String, Object> requestPayload;

	@Column(name = "response_payload", columnDefinition = "TEXT")
	private String responsePayload;

	@Column(nullable = false)
	private String status; // "SUCCESS", "FAILED", "SUCCESS_ALREADY_SET"

	@Column(name = "attempt_count")
	@Builder.Default
	private Integer attemptCount = 1;
}
