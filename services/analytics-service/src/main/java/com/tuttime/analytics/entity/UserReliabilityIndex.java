package com.tuttime.analytics.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_reliability_indexes", schema = "analytics_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserReliabilityIndex extends BaseTimeStampEntity {
	@Id
	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "reliability_score")
	private Integer reliabilityScore;

	@Column(name = "total_bookings")
	private Integer totalBookings;

	@Column(name = "no_show_count")
	private Integer noShowCount;

	@Column(name = "late_cancel_count")
	private Integer lateCancelCount;

	@Column(name = "risk_level", length = 20)
	private String riskLevel;
}