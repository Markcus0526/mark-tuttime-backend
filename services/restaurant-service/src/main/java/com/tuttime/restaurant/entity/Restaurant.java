package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "restaurants")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Restaurant extends BaseTimeStampEntity {

	@Id
	@GeneratedValue
	@Column(name = "restaurant_id", nullable = false, updatable = false)
	private UUID restaurantId;

	@Column(name = "owner_user_id", nullable = false)
	private UUID ownerUserId;

	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "address", nullable = false, length = 512)
	private String address;

	@Column(name = "location", columnDefinition = "geography(Point,4326)")
	private Point location;

	@Column(name = "phone", length = 20)
	private String phone;

	@Column(name = "website", length = 255)
	private String website;

	@Column(name = "rating", precision = 2, scale = 1)
	private BigDecimal rating;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@PrePersist
	public void prePersist() {
		if (rating == null) {
			rating = BigDecimal.ZERO;
		}
	}

}