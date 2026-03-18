package com.tuttime.user.service;

import com.tuttime.user.dto.AdminUpdateUserRequest;
import com.tuttime.user.dto.BlockedUserResponse;
import com.tuttime.user.dto.UserReferralsResponse;
import com.tuttime.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Admin User Service Interface
 * 
 * <p>Provides admin-only operations for user management.
 */
public interface AdminUserService {
    
    /**
     * List users with search and filters
     * @param includeDeleted If true, include soft-deleted users in results
     */
    Page<UserResponse> listUsers(String search, String status, String kycStatus, boolean includeDeleted, Pageable pageable);
    
    /**
     * Get user by ID
     * @param includeDeleted If true, allow getting soft-deleted users
     */
    UserResponse getUserById(Long userId, boolean includeDeleted);
    
    /**
     * Update user information
     */
    UserResponse updateUser(Long userId, AdminUpdateUserRequest request);
    
    /**
     * Block a user
     */
    UserResponse blockUser(Long userId, String reason);
    
    /**
     * Unblock a user
     */
    UserResponse unblockUser(Long userId);
    
    /**
     * Soft delete a user
     */
    void deleteUser(Long userId);
    
    /**
     * Get user referrals
     */
    UserReferralsResponse getUserReferrals(Long userId);
    
    /**
     * Get blocked users
     */
    Page<BlockedUserResponse> getBlockedUsers(Pageable pageable);
    
    /**
     * Hard delete a user (complete delete, not soft delete)
     */
    void hardDeleteUser(Long userId);
    
    /**
     * Get user by phone number
     */
    UserResponse getUserByPhoneNumber(String phoneNumber);
    
    /**
     * Restore a soft-deleted user (clears deletedAt and reactivates)
     */
    UserResponse restoreUser(Long userId);
    
    /**
     * Get preferred languages for multiple users (bulk operation)
     * @param userIds List of user IDs
     * @return Map of userId -> preferred language code (defaults to "en" if not set)
     */
    Map<Long, String> getUsersPreferredLanguages(List<Long> userIds);
    
    /**
     * Get all active user IDs (bulk operation, returns only IDs for performance)
     * @param status Filter by status (e.g., "active")
     * @param includeDeleted If true, include soft-deleted users
     * @return List of user IDs
     */
    List<Long> getAllActiveUserIds(String status, boolean includeDeleted);
}

