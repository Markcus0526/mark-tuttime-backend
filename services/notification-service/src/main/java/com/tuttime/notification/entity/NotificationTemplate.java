package com.tuttime.notification.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.notification.entity.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "notification_templates", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"template_key", "channel", "language"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate extends BaseTimeStampEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(name = "template_key", nullable = false)
	private NotificationTemplateKey templateKey;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "channel", nullable = false)
	private NotificationChannel channel;

	@Column(name = "language", nullable = false, length = 10)
	@Builder.Default
	private String language = "en";

	private String title;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Column(name = "active")
	@Builder.Default
	private boolean active = true;
}
