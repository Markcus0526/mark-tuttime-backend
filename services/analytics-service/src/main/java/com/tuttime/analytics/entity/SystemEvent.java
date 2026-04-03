package com.tuttime.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_events", schema = "analytics_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemEvent {
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