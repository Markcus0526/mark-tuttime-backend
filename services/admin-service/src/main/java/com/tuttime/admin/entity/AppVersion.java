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
 * App Version Entity
 *
 * <p>Stores app version numbers and install links for Android and iOS platforms.
 * Uses singleton pattern - only one active version configuration should exist.
 */
@Entity
@Table(name = "app_version", schema = "admin_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AppVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "version_id")
	private Long versionId;

	@Column(name = "android_version", nullable = false, length = 50)
	private String androidVersion;

	@Column(name = "android_install_link", nullable = false, columnDefinition = "TEXT")
	private String androidInstallLink;

	@Column(name = "ios_version", nullable = false, length = 50)
	private String iosVersion;

	@Column(name = "ios_install_link", nullable = false, columnDefinition = "TEXT")
	private String iosInstallLink;

	@Column(name = "is_active", nullable = false)
	@Builder.Default
	private Boolean isActive = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	@LastModifiedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;

	@Column(name = "updated_by")
	private Long updatedBy;
}

