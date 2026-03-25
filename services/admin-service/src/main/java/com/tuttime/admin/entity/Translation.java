package com.tuttime.admin.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * Translation Entity
 *
 * <p>Represents a translation entry for a specific language and key.
 */
@Entity
@Table(name = "translations", schema = "admin_service",
		uniqueConstraints = @UniqueConstraint(columnNames = {"language_code", "translation_key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Translation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "translation_id")
	private Long translationId;

	@Column(name = "language_code", nullable = false, length = 10)
	private String languageCode;

	@Column(name = "translation_key", nullable = false, length = 255)
	private String translationKey; // e.g., 'common.welcome', 'error.invalid_input'

	@Column(name = "translation_value", nullable = false, columnDefinition = "TEXT")
	private String translationValue; // The actual translated text

	@Column(name = "namespace", length = 100)
	private String namespace; // Optional: group translations (e.g., 'common', 'error', 'ui')

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	@LastModifiedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;

	@Column(name = "created_by")
	private Long createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "language_code", insertable = false, updatable = false)
	private Language language;
}

