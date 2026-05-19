package com.tuttime.analytics.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "branch_yield_managements", schema = "analytics_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchYieldManagement extends BaseTimeStampEntity {

    @EmbeddedId
    private BranchSlotId id;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Column(name = "current_bookings")
    private Integer currentBookings;

    @Column(name = "occupancy_rate", precision = 3, scale = 2)
    private BigDecimal occupancyRate;

    @Column(name = "demand_level", length = 10)
    private String demandLevel;

    @Column(name = "recommended_discount_pct")
    private Float recommendedDiscountPct;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BranchSlotId implements Serializable {
        private UUID branchId;
        private OffsetDateTime timeSlot;
    }
}