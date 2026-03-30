package com.tuttime.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_roles",
		indexes = {
				@Index(name = "idx_user_roles_user_id", columnList = "user_id"),
				@Index(name = "idx_user_roles_role_id", columnList = "role_id")
		})
@Data
@IdClass(UserRoleId.class)
public class UserRole {

	@Id
	@Column(name = "user_id", nullable = false, length = 36)
	private UUID userId;

	@Id
	@Column(name = "role_id", nullable = false)
	private Integer roleId;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("roleId")
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@Column(name = "assigned_at", nullable = false)
	private OffsetDateTime assignedAt = OffsetDateTime.now();

	// Constructors, getters, setters
}