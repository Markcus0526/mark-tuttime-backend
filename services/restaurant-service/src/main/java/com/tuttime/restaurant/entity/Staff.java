package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "staffs", schema = "restaurant_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Staff extends BaseUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "branch_id")
	private Branch branch;

	@Column(name = "user_id", nullable = false)
	private UUID userId; // Link to User Service

	@Column(name = "role_name", nullable = false)
	private String roleName; // "MANAGER", "HOSTESS"

	@Column(name = "active")
	@Builder.Default
	private Boolean active = true;
}
