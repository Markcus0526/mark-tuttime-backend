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
 * Staking Tier Entity
 * 
 * <p>Represents a staking tier that provides AP boost based on staked amount.
 * Example tiers:
 * <ul>
 *   <li>1k APART -> +10 AP/hr</li>
 *   <li>10k APART -> +20 AP/hr</li>
 *   <li>50k APART -> +35 AP/hr</li>
 *   <li>100k APART -> +50 AP/hr</li>
 * </ul>
 */
@Entity
@Table(name = "staking_tiers", schema = "admin_service",
       uniqueConstraints = @UniqueConstraint(columnNames = "min_stake_amount"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StakingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id")
    private Long tierId;

    @Column(name = "tier_name", nullable = false, length = 100)
    private String tierName; // e.g., "Bronze", "Silver", "Gold", "Platinum"

    @Column(name = "min_stake_amount", nullable = false, precision = 36, scale = 18, unique = true)
    private BigDecimal minStakeAmount; // Minimum APART staked to qualify

    @Column(name = "ap_boost_per_hour", nullable = false, precision = 10, scale = 3)
    private BigDecimal apBoostPerHour; // AP boost per hour (e.g., 10, 20, 35, 50)

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

