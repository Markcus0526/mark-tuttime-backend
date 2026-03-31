package com.tuttime.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "restaurants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

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

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		updatedAt = createdAt;
		if (rating == null) {
			rating = BigDecimal.ZERO;
		}
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = Instant.now().atOffset(ZoneOffset.UTC);
	}

}