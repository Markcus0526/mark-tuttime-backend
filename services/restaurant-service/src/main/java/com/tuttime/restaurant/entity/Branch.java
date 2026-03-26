package com.tuttime.restaurant.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@jakarta.persistence.Table(name = "branches", schema = "restaurant_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "branch_id")
	private UUID branchId;

	@Column(name = "restaurant_id", nullable = false, insertable = false, updatable = false)
	private UUID restaurantId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@Column(length = 255, nullable = false)
	private String name;

	@Column(length = 512, nullable = false)
	private String address;

	@Column(length = 20)
	private String phone;

	@Column(length = 50, nullable = false)
	private String timezone;

	@Column(columnDefinition = "geography(Point,4326)")
	@JdbcTypeCode(SqlTypes.GEOMETRY) // Modern alternative to @Type
	private Point location;

	@Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime updatedAt;

	// Getters and setters omitted for brevity

}
