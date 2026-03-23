package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.RestaurantAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Restaurant Audit Log Repository
 */
@Repository
public interface RestaurantAuditLogRepository extends JpaRepository<RestaurantAuditLog, Long> {

    /**
     * Find audit logs by admin ID
     */
    Page<RestaurantAuditLog> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);

    /**
     * Find audit logs by action type
     */
    Page<RestaurantAuditLog> findByActionTypeOrderByCreatedAtDesc(
            RestaurantAuditLog.RestaurantActionType actionType, Pageable pageable);

    /**
     * Find audit logs by resource type and ID
     */
    List<RestaurantAuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType, Long resourceId);

    /**
     * Find audit logs within date range
     */
    @Query("SELECT a FROM RestaurantAuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<RestaurantAuditLog> findByDateRange(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    /**
     * Find audit logs by admin and date range
     */
    @Query("SELECT a FROM RestaurantAuditLog a WHERE a.restaurantId = :restaurantId AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<RestaurantAuditLog> findByRestaurantAndDateRange(
            @Param("restaurantId") Long restaurantId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);
}

