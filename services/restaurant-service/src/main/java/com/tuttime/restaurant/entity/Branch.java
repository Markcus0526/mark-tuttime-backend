package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "branches", schema = "restaurant_service", indexes = {
		@Index(name = "idx_branches_is_deleted", columnList = "is_deleted")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Branch extends BaseTimeStampEntity {

	@Id
	@GeneratedValue
	@Column(name = "branch_id", nullable = false, updatable = false)
	private UUID branchId;

	@Column(name = "restaurant_id", nullable = false)
	private UUID restaurantId;

	@Column(name = "manager_user_id", nullable = false)
	private UUID managerUserId;

	@Column(name = "address", nullable = false, length = 512)
	private String address;

	@Column(name = "phone", length = 20)
	private String phone;

	@Column(name = "timezone", nullable = false, length = 50)
	private String timezone;

	@Column(name = "location", columnDefinition = "geography(Point,4326)")
	private Point location;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;
}
