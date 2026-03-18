package com.tuttime.user.entity.readmodel;

import com.tuttime.user.converter.LocationPrivacyLevelConverter;
import com.tuttime.user.entity.LocationPrivacyLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * CQRS Read Model for User Entity
 *
 * <p>This entity represents a denormalized read model optimized for read operations
 * using the CQRS (Command Query Responsibility Segregation) pattern. It contains
 * pre-computed and aggregated data from multiple sources for faster queries.
 *
 * <p><b>CQRS Pattern:</b>
 * <ul>
 *   <li>Read models are denormalized for optimal read performance</li>
 *   <li>Auto-synced via database triggers or event handlers</li>
 *   <li>Contains aggregated data from multiple services (user, token, mining)</li>
 *   <li>Eventual consistency with main tables</li>
 * </ul>
 *
 * <p><b>Aggregated Data:</b>
 * <p>This read model contains data aggregated from multiple sources:
 * <ul>
 *   <li>User data (email, username, verification status)</li>
 *   <li>Location data (latitude, longitude, privacy settings)</li>
 *   <li>Token balance (totalApartBalance from Token Service)</li>
 *   <li>AP Credits balance (totalApBalance from Mining Service)</li>
 *   <li>User status and activity tracking</li>
 * </ul>
 *
 * <p><b>Database Schema:</b>
 * <p>Stored in the "user_service" schema, table "user_read_model".
 * The table is typically created and maintained via database triggers or
 * event-driven synchronization.
 *
 * <p><b>Note:</b>
 * <p>If IntelliJ shows "Cannot resolve table 'user_read_model'" warning,
 * configure the datasource in IntelliJ (see INTELLIJ_DATASOURCE_SETUP.md).
 * This is just an IDE inspection warning and doesn't affect compilation.
 *
 * @see com.tuttime.user.entity.User
 */
@Entity
@Table(name = "user_read_model", schema = "user_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserReadModel {

    /** User ID (primary key) */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /** User email address */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** Username (required, should be unique) */
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /** User first name */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /** User last name */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /** Profile image URL - full size 1024x1024px (optional) */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /** Profile image tiny thumbnail URL - 32x32px for map markers, compact lists (optional) */
    @Column(name = "profile_image_thumbnail_url", length = 500)
    private String profileImageThumbnailUrl;

    /** Profile image small thumbnail URL - 128x128px for chat headers, profile cards (optional) */
    @Column(name = "profile_image_small_url", length = 500)
    private String profileImageSmallUrl;

    /** Profile image medium thumbnail URL - 512x512px for profile previews, modals (optional) */
    @Column(name = "profile_image_medium_url", length = 500)
    private String profileImageMediumUrl;

    /** User's bio/about section (optional) */
    @Column(name = "about", columnDefinition = "TEXT")
    private String about;

    /** User's websites (comma-separated string) */
    @Column(name = "websites", columnDefinition = "TEXT")
    private String websites;

    /** Instagram username (optional) */
    @Column(name = "instagram", length = 100)
    private String instagram;

    /** Whether the user's email is verified */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    /** Whether the user's email address is verified via email verification code */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /** Whether KYC (Know Your Customer) verification is completed */
    @Column(name = "kyc_completed", nullable = false)
    @Builder.Default
    private Boolean kycCompleted = false;

    /** Whether the user account is active */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Whether the user account is blocked */
    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private Boolean isBlocked = false;

    /** User status level (BRONZE, GOLD, PLATINUM, etc.) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_level", nullable = false)
    @Builder.Default
    private UserStatusLevel statusLevel = UserStatusLevel.BRONZE;

    /** Total APART token balance (aggregated from Token Service) */
    @Column(name = "total_apart_balance", nullable = false, precision = 36, scale = 18)
    @Builder.Default
    private BigDecimal totalApartBalance = BigDecimal.ZERO;

    /** Total AP Credits balance (aggregated from Mining Service) */
    @Column(name = "total_ap_balance", nullable = false, precision = 36, scale = 18)
    @Builder.Default
    private BigDecimal totalApBalance = BigDecimal.ZERO;

    /** Timestamp of last user activity */
    @Column(name = "last_active")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastActive;

    /** Timestamp of last login */
    @Column(name = "last_login")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastLogin;

    /** Date when user registered */
    @Column(name = "registration_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime registrationDate;

    /** Timestamp of last update */
    @Column(name = "last_updated", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastUpdated;

    /** Timestamp when the record was created */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;

    /** Timestamp of last update */
    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;

    /** Timestamp when user was soft-deleted (NULL means user is active) */
    @Column(name = "deleted_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime deletedAt;

    // ==================== Location Fields ====================

    /** Latitude coordinate (-90 to 90) */
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    /** Longitude coordinate (-180 to 180) */
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    /** PostGIS geography point for spatial queries (SRID 4326) */
    @Column(name = "location_point", columnDefinition = "geography(Point, 4326)")
    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    private Point locationPoint;

    /** Location accuracy in meters */
    @Column(name = "location_accuracy")
    private Integer locationAccuracy;

    /** Timestamp when location was last updated */
    @Column(name = "location_updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime locationUpdatedAt;

    /** Whether user has enabled location sharing */
    @Column(name = "location_sharing_enabled", nullable = false)
    @Builder.Default
    private Boolean locationSharingEnabled = false;

    /** Privacy level for location sharing */
    @Convert(converter = LocationPrivacyLevelConverter.class)
    @Column(name = "location_privacy_level", nullable = false, length = 20)
    @Builder.Default
    private LocationPrivacyLevel locationPrivacyLevel = LocationPrivacyLevel.EXACT;

    /** User's gender (optional) */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private com.tuttime.common.constants.Gender gender;

    /**
     * Enumeration of user status levels
     *
     * <p>Users progress through status levels based on their activity,
     * balance, and engagement with the platform. Higher levels may provide
     * additional benefits or privileges.
     */
    public enum UserStatusLevel {
        /** Entry level status */
        BRONZE,
        /** Gold status level */
        GOLD,
        /** Gold Plus status level */
        GOLD_PLUS,
        /** Platinum status level */
        PLATINUM,
        /** Platinum Plus status level */
        PLATINUM_PLUS,
        /** Elite status level (highest tier) */
        ELITE
    }
}

