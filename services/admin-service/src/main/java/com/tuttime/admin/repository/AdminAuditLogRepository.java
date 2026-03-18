package com.tuttime.admin.repository;

import com.tuttime.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Admin Audit Log Repository
 */
@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    /**
     * Find audit logs by admin ID
     */
    Page<AdminAuditLog> findByAdminIdOrderByCreatedAtDesc(Long adminId, Pageable pageable);

    /**
     * Find audit logs by action type
     */
    Page<AdminAuditLog> findByActionTypeOrderByCreatedAtDesc(
            AdminAuditLog.AdminActionType actionType, Pageable pageable);

    /**
     * Find audit logs by resource type and ID
     */
    List<AdminAuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType, Long resourceId);

    /**
     * Find audit logs within date range
     */
    @Query("SELECT a FROM AdminAuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findByDateRange(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    /**
     * Find audit logs by admin and date range
     */
    @Query("SELECT a FROM AdminAuditLog a WHERE a.adminId = :adminId AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findByAdminAndDateRange(
            @Param("adminId") Long adminId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);
}

