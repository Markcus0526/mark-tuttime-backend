package com.tuttime.user.service;

import com.tuttime.user.dto.*;

/**
 * User Management Portal Service Interface
 * 
 * <p>Provides comprehensive user management operations for admin portal.
 */
public interface UserManagementPortalService {
    
    /**
     * Get comprehensive user overview
     * 
     * @param userId The user ID
     * @param conversionPage Page number for conversion history (0-indexed)
     * @param conversionSize Page size for conversion history
     * @param transactionPage Page number for transaction history (0-indexed)
     * @param transactionSize Page size for transaction history
     * @param boostPage Page number for boost history (0-indexed)
     * @param boostSize Page size for boost history
     */
    UserOverview getUserOverview(Long userId, 
                                 Integer conversionPage, Integer conversionSize,
                                 Integer transactionPage, Integer transactionSize,
                                 Integer boostPage, Integer boostSize);

    /**
     * Get extended user detail (without full history)
     * 
     * <p>Returns comprehensive user information including:
     * - Basic info, device info, balances
     * - Mining status, referral info
     * - Account status
     * 
     * <p>This is a lighter version of getUserOverview without history data.
     * 
     * @param userId The user ID
     * @return ExtendedUserDetail with comprehensive user information
     */
    ExtendedUserDetail getExtendedUserDetail(Long userId);
    
    /**
     * Reset user mining
     */
    void resetMining(Long userId, ResetMiningRequest request);
    
    /**
     * Pause user account
     */
    void pauseAccount(Long userId, PauseAccountRequest request);
    
    /**
     * Unpause user account
     */
    void unpauseAccount(Long userId);
    
    /**
     * Force KYC verification status
     */
    void forceKyc(Long userId, ForceKycRequest request);
    
    /**
     * Add bonus to user account
     */
    BonusResponse addBonus(Long userId, AddBonusRequest request);
    
    /**
     * Freeze user withdrawals
     */
    void freezeWithdrawal(Long userId, FreezeWithdrawalRequest request);
    
    /**
     * Unfreeze user withdrawals
     */
    void unfreezeWithdrawal(Long userId);
    
    /**
     * Mark user for anti-fraud watch
     */
    void markAntiFraud(Long userId, MarkAntiFraudRequest request);
    
    /**
     * Remove anti-fraud watch from user
     */
    void unmarkAntiFraud(Long userId);
}

