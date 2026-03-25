package com.tuttime.admin.service;

import com.tuttime.admin.dto.*;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Admin Staking Service Interface
 */
public interface AdminStakingService {
	StakingPositionsResponse getStakingPositions(Long userId);

	List<StakingPoolResponse> listStakingPools();

	// Staking Histories
	Page<StakingHistoryResponse> getStakingHistory(Long poolId, Long userId, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable);

	Page<StakingHistoryResponse> getUnstakingHistory(Long poolId, Long userId, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable);

	// Staking Tier Management (all use staking-service pools)
	StakingPoolResponse getTierById(Long poolId); // Returns pool from staking-service by poolId

	StakingPoolResponse createTier(StakingTierRequest request, Long createdBy);

	StakingPoolResponse updateTier(Long tierId, StakingTierRequest request);

	void deleteTier(Long tierId);

	void reorderTiers(ReorderTiersRequest request); // Note: Reordering may not be applicable for staking-service pools
}

