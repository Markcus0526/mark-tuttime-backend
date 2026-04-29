package com.tuttime.analytics.repository;

import com.tuttime.analytics.entity.FinancialLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialLedgerRepository extends JpaRepository<FinancialLedger, UUID> {

	// Find the daily report for a specific branch
	Optional<FinancialLedger> findByBranchIdAndReportDate(UUID branchId, LocalDate reportDate);

	@Query("SELECT SUM(f.totalGmv) FROM FinancialLedger f")
	BigDecimal sumTotalGmv();

	@Query("SELECT SUM(f.commissionEarned) FROM FinancialLedger f")
	BigDecimal sumTotalCommission();

	@Query("SELECT COUNT(DISTINCT f.branchId) FROM FinancialLedger f")
	long countDistinctBranchId();
}