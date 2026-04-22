package com.tuttime.analytics.repository;

import com.tuttime.analytics.entity.BranchPerformanceStat;
import com.tuttime.analytics.entity.FinancialLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchPerformanceStatRepository extends JpaRepository<BranchPerformanceStat, UUID> {


}