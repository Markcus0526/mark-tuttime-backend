package com.tuttime.user.entity;

import com.tuttime.user.type.PostgreSQLEnumType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * User Device Entity - Tracks user devices for security and anti-fraud purposes
 * 
 * <p>This entity maps to the existing `devices` table in the database.
 * It stores information about devices used by users to access the platform.
 * 
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Device fingerprinting for unique device identification</li>
 *   <li>Client device ID tracking (from mobile app)</li>
 *   <li>IP address tracking for location-based fraud detection</li>
 *   <li>Device type tracking (iOS, Android, Web)</li>
 *   <li>Usage timestamp tracking</li>
 *   <li>Active/inactive device status</li>
 * </ul>
 * 
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>Device fingerprint must be unique per user (enforced by unique constraint)</li>
 *   <li>Maximum 3 devices per user (enforced by AntiFraudService)</li>
 *   <li>Device fingerprint helps detect device spoofing</li>
 *   <li>IP address tracking helps detect multi-account fraud</li>
 * </ul>
 * 
 * <p><b>Database Table:</b>
 * <p>Maps to `devices` table in `user_service` schema.
 * The table uses `device_fingerprint` as the unique identifier per user.
 * A `device_id` column (VARCHAR) should be added to store client-provided device IDs.
 */
@Entity
@Table(name = "devices", schema = "user_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserDevice {
    
    /** Unique identifier for the device record (auto-generated, primary key) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    /** User ID (foreign key to users table) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Client-provided device ID (UUID from mobile app, optional) */
    @Column(name = "client_device_id", length = 255)
    private String clientDeviceId;

    /** Device fingerprint (hash of device characteristics, unique per user) */
    @Column(name = "device_fingerprint", nullable = false, length = 255)
    private String deviceFingerprint;

    /** Device type (iOS, Android, Web) - uses device_type ENUM */

    @Type(PostgreSQLEnumType.class)
    @Column(name = "device_type", nullable = false, columnDefinition = "device_type")
    private DeviceType deviceType;

    /** Device model (e.g., "iPhone 15 Pro", "Samsung Galaxy S24") */
    @Column(name = "device_model", length = 100)
    private String deviceModel;

    /** Operating system version */
    @Column(name = "os_version", length = 50)
    private String osVersion;

    /** Application version */
    @Column(name = "app_version", length = 50)
    private String appVersion;

    /** IP address from which the device was registered (INET type) */
    @Column(name = "ip_address", columnDefinition = "INET")
    @org.hibernate.annotations.ColumnTransformer(
        write = "?::inet",
        read = "ip_address::text"
    )
    private String ipAddress;

    /** Firebase Cloud Messaging (FCM) registration token for push notifications */
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    /** Timestamp when the device was last seen/used */
    @Column(name = "last_seen", nullable = false)
    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastSeen = OffsetDateTime.now(ZoneOffset.UTC);

    /** Whether the device is currently active */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Whether the device is trusted (manually verified) */
    @Column(name = "is_trusted", nullable = false)
    @Builder.Default
    private Boolean isTrusted = false;

    /** Timestamp when the device was first registered */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;

    /** Timestamp when the record was last updated (auto-updated) */
    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;

    /**
     * Device Type Enum - matches database ENUM type
     */
    public enum DeviceType {
        iOS,
        Android,
        Web
    }
}

