package com.tuttime.user.service;

import com.tuttime.user.dto.BlockedUserResponse;

import java.util.List;
import java.util.Set;

/**
 * Service for managing user blocks
 */
public interface UserBlockService {
    
    /**
     * Block a user
     * 
     * @param blockerUserId User ID who is blocking
     * @param blockedUserId User ID to block
     */
    void blockUser(Long blockerUserId, Long blockedUserId);
    
    /**
     * Unblock a user
     * 
     * @param blockerUserId User ID who is unblocking
     * @param blockedUserId User ID to unblock
     */
    void unblockUser(Long blockerUserId, Long blockedUserId);
    
    /**
     * Check if there's a block relationship between two users (bidirectional)
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return true if either user has blocked the other
     */
    boolean isBlocked(Long userId1, Long userId2);
    
    /**
     * Check if a specific user has blocked another user (one-way check)
     * 
     * @param blockerUserId User ID who might have blocked
     * @param blockedUserId User ID who might be blocked
     * @return true if blockerUserId has blocked blockedUserId
     */
    boolean hasBlocked(Long blockerUserId, Long blockedUserId);
    
    /**
     * Get list of user IDs blocked by a user
     * 
     * @param userId User ID
     * @return List of blocked user IDs
     */
    List<Long> getBlockedUserIds(Long userId);
    
    /**
     * Get set of user IDs blocked by a user (for efficient filtering)
     * 
     * @param userId User ID
     * @return Set of blocked user IDs
     */
    Set<Long> getBlockedUserIdsSet(Long userId);
    
    /**
     * Get list of blocked users with details
     * 
     * @param userId User ID
     * @return List of blocked user responses
     */
    List<BlockedUserResponse> getBlockedUsers(Long userId);
}

