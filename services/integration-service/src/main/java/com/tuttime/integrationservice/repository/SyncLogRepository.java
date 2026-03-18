package com.tuttime.integrationservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.tuttime.integrationservice.entity.SyncLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
  List<SyncLog> findByRestaurantIdAndStatusOrderBySyncTimeDesc(Long restaurantId, String status);

  @Query("SELECT s FROM SyncLog s WHERE s.restaurantId = :restaurantId AND s.syncTime > :lastSync ORDER BY s.syncTime DESC")
  List<SyncLog> findRecentSyncs(@Param("restaurantId") Long restaurantId, @Param("lastSync") LocalDateTime lastSync);
}
