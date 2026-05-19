package com.tuttime.user.repository;

import com.tuttime.user.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    // Find all tokens for a user to send multi-device push notifications
    List<UserDevice> findByUserId(UUID userId);

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO user_service.user_devices (user_id, fcm_token, platform, device_name)
            VALUES (:userId, :token, CAST(:platform AS user_service.device_platform), :name)
            ON CONFLICT (fcm_token) 
            DO UPDATE SET 
                last_active_at = CURRENT_TIMESTAMP,
                user_id = EXCLUDED.user_id,
                device_name = EXCLUDED.device_name
            """, nativeQuery = true)
    void upsertToken(
            @Param("userId") UUID userId,
            @Param("token") String token,
            @Param("platform") String platform,
            @Param("name") String name
    );

    @Transactional
    void deleteByFcmToken(String fcmToken);

    List<UserDevice> findAllByUserIdIn(Collection<UUID> userIds);
}
