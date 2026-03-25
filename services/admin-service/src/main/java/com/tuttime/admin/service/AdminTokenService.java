package com.tuttime.admin.service;

import com.tuttime.admin.dto.*;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;

/**
 * Admin Token Service Interface
 */
public interface AdminTokenService {
	TokenBalanceResponse getUserTokenBalance(Long userId);

	TokenBalanceResponse adjustTokenBalance(Long userId, AdjustTokenBalanceRequest request);

	Page<TokenTransactionResponse> getTokenTransactions(Long userId, String type, String status, org.springframework.data.domain.Pageable pageable);

	TokenBalanceResponse burnTokens(Long userId, BurnTokensRequest request);

	UserStatusResponse getUserStatus(Long userId);

	// Bulk Operations
	BulkOperationResponse bulkAdjustTokenBalances(BulkTokenAdjustmentRequest request);

	// Transaction Histories
	Page<GlobalTransactionResponse> getDepositHistory(String status, String network, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable);

	Page<GlobalTransactionResponse> getWithdrawalHistory(String status, String network, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable);

	Page<GlobalTransactionResponse> getBurnHistory(OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable);

	Page<StakingClaimResponse> getClaimHistory(Long userId, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable);
}

