package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_roles",
		indexes = {
				@Index(name = "idx_user_roles_user_id", columnList = "user_id"),
				@Index(name = "idx_user_roles_role_id", columnList = "role_id")
		})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@IdClass(UserRoleId.class)
@EntityListeners(AuditingEntityListener.class)
public class UserRole extends BaseTimeStampEntity {

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
	@Builder.Default
	private OffsetDateTime assignedAt = OffsetDateTime.now();
}