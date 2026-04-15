package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import com.tuttime.restaurant.entity.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "medias", schema = "restaurant_service", indexes = {
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
public class Media extends BaseUuidAnDeleteEntity {

	@Column(name = "entity_id", nullable = false)
	private UUID entityId;

	@Column(name = "url", length = 1024)
	private String url;

	@Column(name = "alt_text", length = 255)
	private String altText;

	@Builder.Default
	@Column(name = "primary_entity")
	private Boolean primaryEntity = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "media_type")
	@JdbcTypeCode(SqlTypes.NAMED_ENUM) // Maps to your PostgreSQL 'media_type_enum'
	private MediaType mediaType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "layout_config", columnDefinition = "jsonb")
	private Map<String, Object> layoutConfig;

	@Builder.Default
	@Column(name = "map_entity")
	private Boolean mapEntity = false;
}