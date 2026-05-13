package com.tuttime.notification.entity;

import com.tuttime.notification.entity.enums.NotificationChannel;
import com.tuttime.notification.entity.enums.NotificationPriority;
import com.tuttime.notification.entity.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "template_id")
	private Integer templateId;

	@Column(name = "template_key", length = 100)
	private String templateKey;

	@Column(name = "event_id", length = 255)
	private UUID eventId;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "channel", nullable = false)
	private NotificationChannel channel;

	@Column(nullable = false)
	private String recipient;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Builder.Default
	private NotificationStatus status = NotificationStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Builder.Default
	private NotificationPriority priority = NotificationPriority.MEDIUM;

	@Column(name = "provider_id")
	private String providerId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "provider_response", columnDefinition = "jsonb")
	private Map<String, Object> providerResponse;

	@Column(name = "retry_count")
	@Builder.Default
	private Integer retryCount = 0;

	@Column(name = "scheduled_at")
	private OffsetDateTime scheduledAt;

	@Column(name = "sent_at")
	private OffsetDateTime sentAt;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@Version
	private Long version;
}
