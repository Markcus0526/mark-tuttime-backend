package com.tuttime.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "event_logs", indexes = {
		@Index(name = "idx_event_log_type_time", columnList = "eventType, timestamp")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLog {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(columnDefinition = "jsonb", nullable = false)
	private String payload;  // Store raw event JSON as String

	@Column(nullable = false)
	private OffsetDateTime timestamp;
}