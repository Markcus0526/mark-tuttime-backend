package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseSerialEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pos_sync_queues", schema = "pos_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PosSyncQueue extends BaseSerialEntity {

	@Column(name = "branch_id", nullable = false)
	private UUID branchId;

	@Column(name = "booking_id")
	private UUID bookingId;

	@Column(name = "order_id")
	private UUID orderId;

	@Column(nullable = false)
	private String action;

	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> payload;

	@Column(nullable = false)
	@Builder.Default
	private String status = "PENDING"; // "PENDING", "COMPLETED", "FAILED_PERMANENTLY"

	@Column(name = "retry_count")
	@Builder.Default
	private Integer retryCount = 0;

	@Column(name = "next_retry_at")
	private OffsetDateTime nextRetryAt;

	@Column(name = "last_error")
	private String lastError;
}
