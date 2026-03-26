package com.tuttime.restaurant.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

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
@jakarta.persistence.Table(name = "zones", schema = "restaurant_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "zone_id")
	private UUID zoneId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "branch_id", nullable = false)
	private Branch branch;

	@Column(length = 255, nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private Integer capacity;

	@Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime updatedAt;

	// Getters and setters omitted for brevity

}
