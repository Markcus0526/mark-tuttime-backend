package com.tuttime.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_locations",
		indexes = {
				@Index(name = "idx_user_locations_gist", columnList = "location")
		})
@Data
public class UserLocation {
	@Id
	@Column(columnDefinition = "uuid", updatable = false, nullable = false)
	private UUID id;

	@JoinColumn(name = "user_id", nullable = false)
	private UUID userId;

	@Column(columnDefinition = "geography(Point,4326)", nullable = false)
	private Point location;

	private Float accuracyMeters;

	@Column(nullable = false)
	@CreationTimestamp
	private OffsetDateTime recordedAt;
}
