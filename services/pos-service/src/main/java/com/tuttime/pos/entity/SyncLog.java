package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sync_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

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