package com.tuttime.payment.repository.projection;

import java.math.BigDecimal;

public interface BranchPaymentSummaryProjection {
	BigDecimal getTodayTotal();
	Long getTodayCount();
	BigDecimal getWeekTotal();
	Long getWeekCount();
	BigDecimal getMonthTotal();
	Long getMonthCount();
	BigDecimal getPendingTotal();
	Long getPendingCount();
	BigDecimal getRefundedTotal();
	Long getRefundedCount();
}