package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import com.tuttime.menu.entity.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "medias", schema = "menu_service", indexes = {
		@Index(name = "idx_medias_deleted", columnList = "deleted"),
		@Index(name = "idx_media_lookup", columnList = "entity_id")
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

}