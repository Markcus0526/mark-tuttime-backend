package com.tuttime.restaurant.entity;

import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "medias", schema = "restaurant_service", indexes = {
		@Index(name = "idx_medias_is_deleted", columnList = "is_deleted"),
		@Index(name = "idx_medias_is_map", columnList = "is_map"),
		@Index(name = "idx_media_lookup", columnList = "entity_type, entity_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

	@Id
	@GeneratedValue
	@Column(name = "media_id", nullable = false, updatable = false)
	private UUID mediaId;

	@Enumerated(EnumType.STRING)
	@Column(name = "entity_type", nullable = false)
	private EntityType entityType;

	@Column(name = "entity_id", nullable = false)
	private UUID entityId;

	@Column(name = "url", length = 1024)
	private String url;

	@Column(name = "alt_text", length = 255)
	private String altText;

	@Builder.Default
	@Column(name = "is_primary")
	private Boolean isPrimary = false;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "media_type")
	private MediaType mediaType = MediaType.IMAGE;

	@Column(name = "svg_content", columnDefinition = "TEXT")
	private String svgContent;

	@Builder.Default
	@Column(name = "is_map")
	private Boolean isMap = false;

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
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = Instant.now().atOffset(ZoneOffset.UTC);
	}

	public void setIsDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}
}