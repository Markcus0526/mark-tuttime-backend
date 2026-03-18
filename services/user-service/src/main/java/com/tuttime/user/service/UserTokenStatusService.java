package com.tuttime.user.service;

import com.tuttime.user.dto.UserTokenHoldingStatus;

/**
 * User Token Status Service Interface
 * 
 * <p>This service provides user token holding status information by integrating
 * with Token Service and Conversion Service.
 */
public interface UserTokenStatusService {
    
    /**
     * Gets the token holding status for a user.
     * 
     * <p>This method:
     * <ol>
     *   <li>Gets current APART balance from Token Service</li>
     *   <li>Gets conversion information from Conversion Service</li>
     *   <li>Calculates current level based on token holdings</li>
     *   <li>Calculates APARTs needed for next level</li>
     *   <li>Gets next conversion time</li>
     *   <li>Gets last month conversion</li>
     * </ol>
     *
     * @param userId User ID
     * @return UserTokenHoldingStatus containing all status information
     */
    UserTokenHoldingStatus getUserTokenHoldingStatus(Long userId);
}

