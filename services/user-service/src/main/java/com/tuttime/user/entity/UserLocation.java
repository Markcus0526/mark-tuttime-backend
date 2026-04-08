package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_locations")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserLocation extends BaseTimeStampEntity {
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

