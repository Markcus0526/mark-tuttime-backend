package com.tuttime.analytics.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "financial_ledgers", schema = "analytics_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialLedger extends BaseUuidEntity {

	@Column(name = "branch_id", nullable = false)
	private UUID branchId;

	@Column(name = "report_date", nullable = false)
	private LocalDate reportDate;

	@Column(name = "total_gmv", precision = 15, scale = 2)
	private BigDecimal totalGmv;

	@Column(name = "commission_earned", precision = 15, scale = 2)
	private BigDecimal commissionEarned;

	@Column(name = "payout_due_restaurant", precision = 15, scale = 2)
	private BigDecimal payoutDueRestaurant;

	@Column(name = "refund_volume", precision = 15, scale = 2)
	private BigDecimal refundVolume;
}
