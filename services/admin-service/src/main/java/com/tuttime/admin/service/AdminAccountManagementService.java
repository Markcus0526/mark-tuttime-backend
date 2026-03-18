package com.tuttime.admin.service;

import com.tuttime.admin.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin Account Management Service Interface
 * 
 * <p>Provides CRUD operations and management features for admin users (admin accounts).
 */
public interface AdminAccountManagementService {

    /**
     * List admin users with filtering and pagination
     * 
     * @param search Search term for username or email (optional)
     * @param role Filter by role (optional)
     * @param isActive Filter by active status (optional)
     * @param pageable Pagination parameters
     * @return Page of admin user summaries
     */
    Page<AdminUserSummaryResponse> listAdminUsers(String search, String role, Boolean isActive, Pageable pageable);

    /**
     * Get admin user details by ID
     * 
     * @param adminId Admin user ID
     * @return Admin user details
     */
    AdminUserResponse getAdminUser(Long adminId);

    /**
     * Create new admin user
     * 
     * @param request Create admin user request
     * @param createdBy ID of admin creating the user
     * @return Created admin user details
     */
    AdminUserResponse createAdminUser(CreateAdminUserRequest request, Long createdBy);

    /**
     * Update admin user
     * 
     * @param adminId Admin user ID to update
     * @param request Update request
     * @param updatedBy ID of admin performing the update
     * @return Updated admin user details
     */
    AdminUserResponse updateAdminUser(Long adminId, UpdateAdminUserRequest request, Long updatedBy);

    /**
     * Delete admin user
     * 
     * @param adminId Admin user ID to delete
     * @param deletedBy ID of admin performing the deletion
     */
    void deleteAdminUser(Long adminId, Long deletedBy);

    /**
     * Activate admin user
     * 
     * @param adminId Admin user ID
     * @param updatedBy ID of admin performing the action
     * @return Updated admin user details
     */
    AdminUserResponse activateAdminUser(Long adminId, Long updatedBy);

    /**
     * Deactivate admin user
     * 
     * @param adminId Admin user ID
     * @param updatedBy ID of admin performing the action
     * @return Updated admin user details
     */
    AdminUserResponse deactivateAdminUser(Long adminId, Long updatedBy);

    /**
     * Unlock admin user account
     * 
     * @param adminId Admin user ID
     * @param updatedBy ID of admin performing the action
     * @return Updated admin user details
     */
    AdminUserResponse unlockAdminUser(Long adminId, Long updatedBy);

    /**
     * Reset failed login attempts
     * 
     * @param adminId Admin user ID
     * @param updatedBy ID of admin performing the action
     */
    void resetFailedLoginAttempts(Long adminId, Long updatedBy);

    /**
     * Change password (self-service)
     * 
     * @param adminId Admin user ID
     * @param request Change password request
     */
    void changePassword(Long adminId, ChangePasswordRequest request);

    /**
     * Reset password (admin-initiated)
     * 
     * @param adminId Admin user ID
     * @param request Reset password request
     * @param resetBy ID of admin performing the reset
     */
    void resetPassword(Long adminId, ResetPasswordRequest request, Long resetBy);
}

