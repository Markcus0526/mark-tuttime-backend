package com.tuttime.restaurant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "tables", schema = "restaurant_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Table {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "table_id")
	private UUID tableId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "zone_id", nullable = false)
	private Zone zone;

	@Column(length = 50, nullable = false)
	private String number;

	@Column(nullable = false)
	private Integer seats;

	@Builder.Default
	@Column(length = 20, nullable = false)
	private String status = "active";

	@Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime updatedAt;

	// Getters and setters omitted for brevity

}
