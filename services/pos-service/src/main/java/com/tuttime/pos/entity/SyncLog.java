package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseSerialEntity;
import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sync_logs")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SyncLog extends BaseSerialEntity {

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "restaurant_id", nullable = false)
	private UUID restaurantId;

	@Column(nullable = false)
	private String status; // e.g., "SUCCESS", "HARD_FAILURE", "PENDING"

	@Column(name = "event_type")
	private String eventType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private Map<String, String> metadata;

	@Column(name = "error_message")
	private String errorMessage;
}