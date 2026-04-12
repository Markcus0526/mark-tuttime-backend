package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseDeleteEntity;
import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.restaurant.entity.enums.EntityType;
import com.tuttime.restaurant.entity.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "medias", schema = "restaurant_service", indexes = {
		@Index(name = "idx_medias_deleted", columnList = "deleted"),
		@Index(name = "idx_medias_map_entity", columnList = "map_entity"),
		@Index(name = "idx_media_lookup", columnList = "entity_type, entity_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Media extends BaseDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
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
	@Column(name = "primary_entity")
	private Boolean primaryEntity = false;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "media_type")
	private MediaType mediaType = MediaType.IMAGE;

	@Column(name = "svg_content", columnDefinition = "TEXT")
	private String svgContent;

	@Builder.Default
	@Column(name = "map_entity")
	private Boolean mapEntity = false;
}