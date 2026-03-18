package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Extended User Detail Response
 * 
 * <p>Contains comprehensive user information including basic details,
 * device info, mining status, and referral information.
 * This is a lighter version of UserOverview without full history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendedUserDetail {
    
    /** Basic user information */
    private UserBasicInfo basicInfo;
    
    /** Device information */
    private DeviceInfo deviceInfo;
    
    /** User balances (AP and APART) */
    private UserBalances balances;
    
    /** Mining status and information */
    private UserMiningInfo mining;
    
    /** Referral information */
    private UserReferralInfo referrals;
    
    /** Account status flags */
    private AccountStatus accountStatus;
}

