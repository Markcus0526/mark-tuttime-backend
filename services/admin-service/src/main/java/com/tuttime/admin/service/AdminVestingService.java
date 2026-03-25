package com.tuttime.admin.service;

import com.tuttime.admin.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Admin Vesting Service Interface
 *
 * <p>TODO: Integrate with EVM vesting contract for actual token operations.
 * Current implementation provides placeholder methods that should be connected
 * to blockchain contract interactions.
 */
public interface AdminVestingService {

	// Pool Management
	Page<VestingPoolResponse> listPools(Pageable pageable);

	VestingPoolResponse getPool(Long poolId);

	VestingPoolResponse createPool(VestingPoolRequest request);

	VestingPoolResponse updatePool(Long poolId, VestingPoolRequest request);

	VestingPoolResponse getPoolBalance(Long poolId);

	VestingPoolResponse fundPool(Long poolId, BigDecimal amount);

	// Investor Management
	Page<InvestorResponse> listInvestors(Long poolId, Pageable pageable);

	InvestorResponse addInvestor(Long poolId, InvestorRequest request);

	ClaimStatusResponse getInvestorClaimStatus(Long poolId, Long investorId);
}

