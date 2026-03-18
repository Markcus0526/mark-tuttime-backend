package com.tuttime.user.entity;

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

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

/**
 * User Entity - Represents a user in the APART TOKEN Platform.
 *
 * <p>This entity stores all user-related information including authentication
 * credentials, profile data, verification status, and account state.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Automatic timestamp management via JPA Auditing</li>
 *   <li>Soft delete support (deletedAt field)</li>
 *   <li>KYC status tracking</li>
 *   <li>Account blocking functionality</li>
 * </ul>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>Password is stored as hash (never store plain passwords)</li>
 *   <li>Email is required and should be unique</li>
 *   <li>Phone number is optional but validated if provided</li>
 * </ul>
 *
 */
@Entity
@Table(name = "users", schema = "user_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    /** Unique identifier for the user (auto-generated) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /** First name (optional, can be null) */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /** Last name (optional, can be null) */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /** Email address (required, should be unique) */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** Username (required, should be unique) */
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /** Phone number (optional, validated if provided) */
    @Column(name = "phone", length = 20)
    private String phone;

    /** Telegram username (optional) */
    @Column(name = "telegram", length = 100)
    private String telegram;

    /** Instagram username (optional) */
    @Column(name = "instagram", length = 100)
    private String instagram;

    /** Hashed password (never store plain passwords) */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** User's country (optional) */
    @Column(name = "country", length = 100)
    private String country;

    /** User's city (optional) */
    @Column(name = "city", length = 100)
    private String city;

    /** User's date of birth (truncated to start of day in UTC). Always use UTC when setting this value. */
    @Column(name = "date_of_birth")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime dateOfBirth;

    /** User's gender (optional) */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private com.tuttime.common.constants.Gender gender;

    /** User preferred language code (e.g., en, ko, ja, zh-CN) for notification translation */
    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "en";

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

    /** Timestamp when KYC verification was completed */
    @Column(name = "kyc_verified_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime kycVerifiedAt;

    /** Date when the user registered (auto-set, cannot be updated) */
    @Column(name = "registration_date", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime registrationDate;

    /** Last time the user was active on the platform */
    @Column(name = "last_active")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastActive;

    /** Last time the user logged in */
    @Column(name = "last_login")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastLogin;

    /** Whether the user account is active (can be deactivated without blocking) */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Whether the user account is blocked (prevents all access) */
    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private Boolean isBlocked = false;

    /** Reason why the account was blocked (if blocked) */
    @Column(name = "blocked_reason", columnDefinition = "TEXT")
    private String blockedReason;

    /** Timestamp when the account was blocked */
    @Column(name = "blocked_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime blockedAt;

    /** Whether the account is paused (temporarily disabled, can be reactivated) */
    @Column(name = "is_paused", nullable = false)
    @Builder.Default
    private Boolean isPaused = false;

    /** Whether withdrawals are frozen for this account */
    @Column(name = "withdrawal_frozen", nullable = false)
    @Builder.Default
    private Boolean withdrawalFrozen = false;

    /** Whether the account is marked for anti-fraud watch */
    @Column(name = "anti_fraud_watch", nullable = false)
    @Builder.Default
    private Boolean antiFraudWatch = false;

    /** Timestamp when the record was created (auto-set, cannot be updated) */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;

    /** Timestamp when the record was last updated (auto-updated) */
    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;

    /** Timestamp when the record was soft-deleted (null if not deleted) */
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
    @Convert(converter = com.tuttime.user.converter.LocationPrivacyLevelConverter.class)
    @Column(name = "location_privacy_level", nullable = false, length = 20)
    @Builder.Default
    private LocationPrivacyLevel locationPrivacyLevel = LocationPrivacyLevel.EXACT;

    // ==================== Apart-sharing Integration Fields ====================

    /** Whether the user has an account on Apart-sharing platform (cached from GraphQL API) */
    @Column(name = "apart_sharing_registered")
    private Boolean apartSharingRegistered;

    /** Whether the user is verified on Apart-sharing platform (cached from GraphQL API) */
    @Column(name = "apart_sharing_verified")
    private Boolean apartSharingVerified;

    /** Status from Apart-sharing platform (cached from GraphQL API) */
    @Column(name = "apart_sharing_status", length = 50)
    private String apartSharingStatus;

    /** Timestamp when Apart-sharing status was last checked/updated */
    @Column(name = "apart_sharing_status_checked_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime apartSharingStatusCheckedAt;

    /** Full response data from Apart-sharing GraphQL API (JSON) for future use */
    @Column(name = "apart_sharing_status_data", columnDefinition = "jsonb")
    private String apartSharingStatusData;
}

