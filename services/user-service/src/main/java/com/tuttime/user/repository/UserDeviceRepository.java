package com.tuttime.user.repository;

import com.tuttime.user.entity.UserDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserDevice entity
 * 
 * <p>Provides data access methods for user device tracking and anti-fraud operations.
 */
@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    
    /**
     * Find device by client device ID (UUID from mobile app)
     * 
     * @param clientDeviceId Client device ID to search for
     * @return Optional containing the device if found
     */
    Optional<UserDevice> findByClientDeviceId(String clientDeviceId);

    /**
     * Find device by device fingerprint
     * 
     * @param deviceFingerprint Device fingerprint to search for
     * @return Optional containing the device if found
     */
    Optional<UserDevice> findByDeviceFingerprint(String deviceFingerprint);

    /**
     * Find all devices for a specific user
     * 
     * @param userId User ID to search for
     * @return List of devices associated with the user
     */
    List<UserDevice> findByUserId(Long userId);

    /**
     * Find active devices for a specific user
     * 
     * @param userId User ID to search for
     * @return List of active devices associated with the user
     */
    List<UserDevice> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Check if a device fingerprint is used by a different user
     * 
     * <p>This is used to detect if the same device is being used by multiple users,
     * which could indicate multi-account fraud.
     * 
     * @param deviceFingerprint Device fingerprint to check
     * @param userId User ID to exclude from the check
     * @return true if device is used by another user, false otherwise
     */
    @Query("SELECT COUNT(d) > 0 FROM UserDevice d WHERE d.deviceFingerprint = :deviceFingerprint AND d.userId != :userId AND d.isActive = true")
    boolean existsByDeviceFingerprintAndUserIdNot(@Param("deviceFingerprint") String deviceFingerprint, @Param("userId") Long userId);

    /**
     * Check if a client device ID is used by a different user
     * 
     * @param clientDeviceId Client device ID to check
     * @param userId User ID to exclude from the check
     * @return true if device is used by another user, false otherwise
     */
    @Query("SELECT COUNT(d) > 0 FROM UserDevice d WHERE d.clientDeviceId = :clientDeviceId AND d.clientDeviceId IS NOT NULL AND d.userId != :userId AND d.isActive = true")
    boolean existsByClientDeviceIdAndUserIdNot(@Param("clientDeviceId") String clientDeviceId, @Param("userId") Long userId);

    /**
     * Count active devices per user
     * 
     * @param userId User ID to count devices for
     * @return Number of active devices for the user
     */
    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.userId = :userId AND d.isActive = true")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Find device by user ID and client device ID
     * 
     * @param userId User ID
     * @param clientDeviceId Client device ID
     * @return Optional containing the device if found
     */
    Optional<UserDevice> findByUserIdAndClientDeviceId(Long userId, String clientDeviceId);

    /**
     * Find device by user ID and device fingerprint
     * 
     * @param userId User ID
     * @param deviceFingerprint Device fingerprint
     * @return Optional containing the device if found
     */
    Optional<UserDevice> findByUserIdAndDeviceFingerprint(Long userId, String deviceFingerprint);

    /**
     * Find all devices with a specific IP address
     * 
     * @param ipAddress IP address to search for
     * @return List of devices with the specified IP address
     */
    List<UserDevice> findByIpAddress(String ipAddress);

    /**
     * Find all blocked devices (isActive = false) with pagination
     * 
     * @param pageable Pagination parameters
     * @return Page of blocked devices
     */
    Page<UserDevice> findByIsActiveFalse(Pageable pageable);
    
    /**
     * Find FCM tokens for active devices of a user
     * 
     * @param userId User ID
     * @return List of FCM tokens for active devices
     */
    @Query("SELECT d.fcmToken FROM UserDevice d WHERE d.userId = :userId AND d.isActive = true AND d.fcmToken IS NOT NULL")
    List<String> findFcmTokensByUserIdAndIsActiveTrue(@Param("userId") Long userId);
    
    /**
     * Find devices by user ID and FCM token
     * 
     * @param userId User ID
     * @param fcmToken FCM token to search for
     * @return List of devices with the specified FCM token
     */
    @Query("SELECT d FROM UserDevice d WHERE d.userId = :userId AND d.fcmToken = :fcmToken")
    List<UserDevice> findByUserIdAndFcmToken(@Param("userId") Long userId, @Param("fcmToken") String fcmToken);
}

