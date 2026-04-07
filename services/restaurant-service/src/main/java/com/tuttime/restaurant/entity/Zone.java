package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "zones", schema = "restaurant_service", indexes = {
		@Index(name = "idx_zones_is_deleted", columnList = "is_deleted")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Zone extends BaseTimeStampEntity {

	@Id
	@GeneratedValue
	@Column(name = "zone_id", nullable = false, updatable = false)
	private UUID zoneId;

	@Column(name = "branch_id", nullable = false)
	private UUID branchId;

	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "capacity", nullable = false)
	private Integer capacity;

	@Builder.Default
	@Column(name = "is_closed", nullable = false)
	private boolean isClosed = false;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

}