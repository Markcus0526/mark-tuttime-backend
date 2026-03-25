package com.tuttime.restaurant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@jakarta.persistence.Table(name = "medias", schema = "restaurant_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "media_id")
	private Long mediaId;

	@Column(name = "entity_type", length = 50, nullable = false)
	private EntityType entityType; // 'RESTAURANT', 'BRANCH', 'ZONE', 'TABLE'

	@Column(name = "entity_id", nullable = false)
	private Long entityId;

	@Column(length = 1024, nullable = false)
	private String url;

	@Column(name = "alt_text", length = 255)
	private String altText;

	@Builder.Default
	@Column(name = "is_primary")
	private Boolean isPrimary = false;

	@Builder.Default
	@Column(name = "media_type", length = 50)
	private MediaType mediaType = MediaType.IMAGE;

	@Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = OffsetDateTime.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = OffsetDateTime.now();
	}

}
