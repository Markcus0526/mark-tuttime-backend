package com.tuttime.restaurant.entity.readmodel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tuttime.restaurant.entity.Branch;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

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
 * @see com.tuttime.restaurant.entity.Restaurant
 */
@Entity
@Table(name = "user_read_model", schema = "user_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RestaurantReadModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "address", length = 500)
    private String address;

    /** User's country (optional) */
    @Column(name = "country", length = 100)
    private String country;

    /** User's city (optional) */
    @Column(name = "city", length = 100)
    private String city;

    // ==================== Location Fields ====================

    /** Latitude coordinate (-90 to 90) */
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    /** Longitude coordinate (-180 to 180) */
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    /** Location accuracy in meters */
    @Column(name = "location_accuracy")
    private Integer locationAccuracy;

    // ==================== Contact Fields ====================

    /** Whether the user's email address is verified via email verification code */
    @Column(name = "email", length = 100)
    private String email;

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


    @Column(name = "rating")
    private BigDecimal rating = BigDecimal.ZERO;

    /** User preferred language code (e.g., en, ko, ja, zh-CN) for notification translation */
    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "en";

    /** User's bio/about section (optional) */
    @Column(name = "about", columnDefinition = "TEXT")
    private String about;

    /** Profile image URL - full size 1024x1024px (optional) */
    @Column(name = "about_image_url", length = 500)
    private String profileImageUrl;

    /** Profile image tiny thumbnail URL - 32x32px for map markers, compact lists (optional) */
    @Column(name = "about_image_thumbnail_url", length = 500)
    private String profileImageThumbnailUrl;

    /** Profile image small thumbnail URL - 128x128px for chat headers, profile cards (optional) */
    @Column(name = "about_image_small_url", length = 500)
    private String profileImageSmallUrl;

    /** Profile image medium thumbnail URL - 512x512px for profile previews, modals (optional) */
    @Column(name = "about_image_medium_url", length = 500)
    private String profileImageMediumUrl;

    /** User's websites (comma-separated string) */
    @Column(name = "websites", columnDefinition = "TEXT")
    private String websites;


    /** Whether the user's email is verified */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    /** Date when the user registered (auto-set, cannot be updated) */
    @Column(name = "registration_date", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime registrationDate;

    /** Last time the user was active on the platform */
    @Column(name = "last_active")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastActive;

    @Column(name = "last_login_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastLoginAt;

    /** Whether the user account is active (can be deactivated without blocking) */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lockedUntil;

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

    @OneToMany
    @JoinColumn(name = "restaurant_id")
    private List<Branch> branches;
}

