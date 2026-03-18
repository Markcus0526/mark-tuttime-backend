package com.tuttime.admin.entity;

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

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Status Level Entity
 * 
 * <p>Represents a user status level that provides AP boost based on APART balance.
 * Example levels:
 * <ul>
 *   <li>Silver / 5k -> +2 AP/hr</li>
 *   <li>Gold / 20k -> +3 AP/hr</li>
 *   <li>Platinum / 50k -> +5 AP/hr + pro auto-mining</li>
 *   <li>Elite / 100k -> +7.77 AP/hr + pro auto-mining</li>
 *   <li>XXX / ??? -> +10 AP/hr + pro auto-mining</li>
 * </ul>
 */
@Entity
@Table(name = "status_levels", schema = "admin_service",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "level_name"),
           @UniqueConstraint(columnNames = "min_apart_balance")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StatusLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "level_id")
    private Long levelId;

    @Column(name = "level_name", nullable = false, unique = true, length = 50)
    private String levelName; // e.g., "SILVER", "GOLD", "PLATINUM", "ELITE", "XXX"

    @Column(name = "min_apart_balance", nullable = false, precision = 36, scale = 18, unique = true)
    private BigDecimal minApartBalance; // Minimum APART balance to qualify

    @Column(name = "ap_boost_per_hour", nullable = false, precision = 10, scale = 3)
    private BigDecimal apBoostPerHour; // AP boost per hour (e.g., 2, 3, 5, 7.77, 10)

    @Column(name = "includes_pro_auto_mining", nullable = false)
    @Builder.Default
    private Boolean includesProAutoMining = false; // Whether this level includes pro auto-mining

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0; // Order for display (lower = higher priority)

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}

