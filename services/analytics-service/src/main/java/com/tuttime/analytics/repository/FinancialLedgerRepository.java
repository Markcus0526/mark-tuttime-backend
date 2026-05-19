package com.tuttime.analytics.repository;

import com.tuttime.analytics.entity.FinancialLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    // Daily AOV for the last 'limit' days (limit = number of days)
    @Query(value = "SELECT DATE(report_date) AS period, AVG(total_gmv / order_count) AS aov " +
            "FROM financial_ledgers WHERE branch_id = :branchId " +
            "GROUP BY DATE(report_date) ORDER BY report_date DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findDailyAovNative(@Param("branchId") UUID branchId, @Param("limit") int limit);

    // Weekly AOV for the last 'limit' weeks
    @Query(value = "SELECT CONCAT(EXTRACT(YEAR FROM report_date)::text, '-W', EXTRACT(WEEK FROM report_date)::text) AS period, " +
            "AVG(total_gmv / order_count) AS aov " +
            "FROM financial_ledgers WHERE branch_id = :branchId " +
            "GROUP BY EXTRACT(YEAR FROM report_date), EXTRACT(WEEK FROM report_date) " +
            "ORDER BY MAX(report_date) DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findWeeklyAovNative(@Param("branchId") UUID branchId, @Param("limit") int limit);
}