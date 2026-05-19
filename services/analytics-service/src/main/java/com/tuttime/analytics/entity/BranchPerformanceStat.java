package com.tuttime.analytics.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "branch_performance_stats", schema = "analytics_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchPerformanceStat extends BaseTimeStampEntity {
    @Id
    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "avg_prep_time_sec")
    private Integer avgPrepTimeSec;

    @Column(name = "pos_sync_health_pct", precision = 5, scale = 2)
    private BigDecimal posSyncHealthPct;

    @Column(name = "avg_guest_rating", precision = 3, scale = 2)
    private BigDecimal avgGuestRating;

    @Column(name = "popular_dish_id")
    private UUID popularDishId;

    @Column(name = "last_event_id")
    private UUID lastEventId;
}
