package com.tuttime.notification.entity;

import com.tuttime.notification.entity.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notification_preferences", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"user_id", "notification_type", "channel"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "notification_type", nullable = false, length = 50)
	private String notificationType;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "channel", nullable = false)
	private NotificationChannel channel;

	@Column(name = "enabled")
	@Builder.Default
	private boolean enabled = true;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;
}
