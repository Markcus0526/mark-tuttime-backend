package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseDeleteEntity;
import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "restaurant_staffs", schema = "restaurant_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RestaurantStaff extends BaseTimeStampEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "staff_id")
	private UUID staffId;

	@JoinColumn(name = "restaurant_id", nullable = false)
	private UUID restaurantId;

	@JoinColumn(name = "branch_id")
	private UUID branchId; // If NULL, user is a Restaurant-level staff

	@Column(name = "user_id", nullable = false)
	private UUID userId; // Link to User Service

	@Column(name = "role_name", nullable = false)
	private String roleName; // "MANAGER", "HOSTESS"

	@Column(name = "is_active")
	@Builder.Default
	private boolean isActive = true;
}
