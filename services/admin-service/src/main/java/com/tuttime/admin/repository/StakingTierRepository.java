package com.tuttime.admin.repository;

import com.tuttime.admin.entity.StakingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Staking Tier Repository
 */
@Repository
public interface StakingTierRepository extends JpaRepository<StakingTier, Long> {

    List<StakingTier> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<StakingTier> findAllByOrderByDisplayOrderAsc();

    Optional<StakingTier> findByMinStakeAmount(BigDecimal minStakeAmount);

    boolean existsByMinStakeAmount(BigDecimal minStakeAmount);
}

