package com.tuttime.analytics.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_events", schema = "analytics_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SystemEvent extends BaseTimeStampEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "service_name")
	private String serviceName;

	@Column(name = "event_type")
	private String eventType;

	@Column(name = "entity_id")
	private UUID entityId;

	@JdbcTypeCode(SqlTypes.JSON)
	private Object payload; // Flexible payload storage

	@Column(name = "occurred_at")
	@Builder.Default
	private OffsetDateTime occurredAt = OffsetDateTime.now();
}