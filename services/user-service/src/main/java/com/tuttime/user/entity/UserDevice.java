package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import com.tuttime.user.entity.enums.DevicePlatform;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_devices", schema = "user_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice extends BaseUuidEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "fcm_token", nullable = false, unique = true)
	private String fcmToken;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DevicePlatform platform;

	@Column(name = "device_name")
	private String deviceName;

	@Column(name = "last_active_at")
	private OffsetDateTime lastActiveAt;
}

