package com.tuttime.restaurant.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurants", schema = "restaurant_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Restaurant {

	@Id
	@Column(name = "restaurant_id")
	private UUID restaurantId;

	@Column(name = "owner_user_id", nullable = false)
	private UUID ownerUserId;

	@Column(length = 255, nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(length = 512, nullable = false)
	private String address;

	@Column(columnDefinition = "geography(Point,4326)")
	@JdbcTypeCode(SqlTypes.GEOMETRY) // Modern alternative to @Type
	private Point location;

	@Column(length = 20)
	private String phone;

	@Column(length = 255)
	private String website;

	@Column(precision = 2, scale = 1)
	private BigDecimal rating;

	@Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime updatedAt;
}