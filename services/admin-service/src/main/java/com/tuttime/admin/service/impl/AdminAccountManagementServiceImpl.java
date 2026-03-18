package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.entity.AdminAuditLog;
import com.tuttime.admin.entity.AdminUser;
import com.tuttime.admin.repository.AdminUserRepository;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminAccountManagementService;
import com.tuttime.admin.util.PasswordValidator;
import com.tuttime.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin Account Management Service Implementation
 * 
 * <p>Provides CRUD operations and management features for admin users (admin accounts).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccountManagementServiceImpl implements AdminAccountManagementService {

    private final AdminUserRepository adminUserRepository;
    private final AdminAuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> listAdminUsers(String search, String role, Boolean isActive, Pageable pageable) {
        log.debug("Listing admin users: search={}, role={}, isActive={}", search, role, isActive);

        AdminUser.AdminRole roleEnum = null;
        if (role != null && !role.isEmpty()) {
            try {
                roleEnum = AdminUser.AdminRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid role: " + role);
            }
        }

        Page<AdminUser> adminUsers;

        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.trim();
            if (roleEnum != null && isActive != null) {
                adminUsers = adminUserRepository.findByRoleAndIsActiveAndUsernameOrEmailContaining(
                        roleEnum, isActive, searchTerm, pageable);
            } else if (roleEnum != null) {
                adminUsers = adminUserRepository.findByRoleAndUsernameOrEmailContaining(
                        roleEnum, searchTerm, pageable);
            } else if (isActive != null) {
                adminUsers = adminUserRepository.findByIsActiveAndUsernameOrEmailContaining(
                        isActive, searchTerm, pageable);
            } else {
                adminUsers = adminUserRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        searchTerm, pageable);
            }
        } else {
            if (roleEnum != null && isActive != null) {
                adminUsers = adminUserRepository.findByRoleAndIsActive(roleEnum, isActive, pageable);
            } else if (roleEnum != null) {
                adminUsers = adminUserRepository.findByRole(roleEnum, pageable);
            } else if (isActive != null) {
                adminUsers = adminUserRepository.findByIsActive(isActive, pageable);
            } else {
                adminUsers = adminUserRepository.findAll(pageable);
            }
        }

        return adminUsers.map(this::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getAdminUser(Long adminId) {
        log.debug("Getting admin user: adminId={}", adminId);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin user not found: " + adminId));

        return toResponse(admin);
    }

    @Override
    @Transactional
    public AdminUserResponse createAdminUser(CreateAdminUserRequest request, Long createdBy) {
        log.info("Creating admin user: username={}, email={}, role={}, createdBy={}",
                request.getUsername(), request.getEmail(), request.getRole(), createdBy);

        // Validate password strength
        if (!PasswordValidator.isValid(request.getPassword())) {
            String error = PasswordValidator.getValidationError(request.getPassword());
            throw new BusinessException(error != null ? error : "Password does not meet security requirements");
        }

        // Check username uniqueness
        if (adminUserRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists: " + request.getUsername());
        }

        // Check email uniqueness
        if (adminUserRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists: " + request.getEmail());
        }

        // Encode password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create admin user
        AdminUser admin = AdminUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .role(request.getRole())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .failedLoginAttempts(0)
                .createdBy(createdBy)
                .build();

        admin = adminUserRepository.save(admin);

        // Get creator username for audit
        String creatorUsername = getAdminUsername(createdBy);

        // Log audit
        auditService.logAction(createdBy, creatorUsername,
                AdminAuditLog.AdminActionType.ADMIN_USER_CREATED,
                "ADMIN_USER", admin.getAdminId(),
                String.format("Created admin user: username=%s, role=%s", admin.getUsername(), admin.getRole()));

        log.info("Admin user created successfully: adminId={}, username={}", admin.getAdminId(), admin.getUsername());

        return toResponse(admin);
    }

    @Override
    @Transactional
    public AdminUserResponse updateAdminUser(Long adminId, UpdateAdminUserRequest request, Long updatedBy) {
        log.info("Updating admin user: adminId={}, updatedBy={}", adminId, updatedBy);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin user not found: " + adminId));

        // Self-protection: Check if updating own account
        boolean isSelfUpdate = adminId.equals(updatedBy);
        AdminUser updater = adminUserRepository.findById(updatedBy)
                .orElseThrow(() -> new BusinessException("Updater admin not found: " + updatedBy));

        // Check role hierarchy
        if (request.getRole() != null && !request.getRole().equals(admin.getRole())) {
            validateRoleChange(admin, request.getRole(), updater, isSelfUpdate);
        }

        // Update fields
        boolean changed = false;
        if (request.getUsername() != null && !request.getUsername().equals(admin.getUsername())) {
            if (adminUserRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException("Username already exists: " + request.getUsername());
            }
            admin.setUsername(request.getUsername());
            changed = true;
        }

        if (request.getEmail() != null && !request.getEmail().equals(admin.getEmail())) {
            if (adminUserRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Email already exists: " + request.getEmail());
            }
            admin.setEmail(request.getEmail());
            changed = true;
        }

        if (request.getRole() != null && !request.getRole().equals(admin.getRole())) {
            admin.setRole(request.getRole());
            changed = true;
        }

        if (request.getIsActive() != null && !request.getIsActive().equals(admin.getIsActive())) {
            // Self-protection: Cannot deactivate self
            if (isSelfUpdate && !request.getIsActive()) {
                throw new BusinessException("Cannot deactivate your own account");
            }
            admin.setIsActive(request.getIsActive());
            changed = true;
        }

        if (!changed) {
            log.debug("No changes detected for admin user: adminId={}", adminId);
            return toResponse(admin);
        }

        admin = adminUserRepository.save(admin);

        // Log audit
        auditService.logAction(updatedBy, updater.getUsername(),
                AdminAuditLog.AdminActionType.ADMIN_USER_UPDATED,
                "ADMIN_USER", adminId,
                String.format("Updated admin user: username=%s", admin.getUsername()));

        log.info("Admin user updated successfully: adminId={}", adminId);

        return toResponse(admin);
    }

    @Override
    @Transactional
    public void deleteAdminUser(Long adminId, Long deletedBy) {
        log.info("Deleting admin user: adminId={}, deletedBy={}", adminId, deletedBy);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin user not found: " + adminId));

        // Self-protection: Cannot delete self
        if (adminId.equals(deletedBy)) {
            throw new BusinessException("Cannot delete your own account");
        }

        AdminUser deleter = adminUserRepository.findById(deletedBy)
                .orElseThrow(() -> new BusinessException("Deleter admin not found: " + deletedBy));

        String username = admin.getUsername();

        adminUserRepository.delete(admin);

        // Log audit
        auditService.logAction(deletedBy, deleter.getUsername(),
                AdminAuditLog.AdminActionType.ADMIN_USER_DELETED,
                "ADMIN_USER", adminId,
                String.format("Deleted admin user: username=%s", username));

        log.info("Admin user deleted successfully: adminId={}, username={}", adminId, username);
    }

    @Override
    @Transactional
    public AdminUserResponse activateAdminUser(Long adminId, Long updatedBy) {
        log.info("Activating admin user: adminId={}, updatedBy={}", adminId, updatedBy);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin user not found: " + adminId));

        if (admin.getIsActive()) {
            log.debug("Admin user already active: adminId={}", adminId);
            return toResponse(admin);
        }

        admin.setIsActive(true);
        admin = adminUserRepository.save(admin);

        AdminUser updater = adminUserRepository.findById(updatedBy)
                .orElseThrow(() -> new BusinessException("Updater admin not found: " + updatedBy));

        // Log audit
        auditService.logAction(updatedBy, updater.getUsername(),
                AdminAuditLog.AdminActionType.ADMIN_USER_ACTIVATED,
                "ADMIN_USER", adminId,
                String.format("Activated admin user: username=%s", admin.getUsername()));

        return toResponse(admin);
    }

    @Override
    @Transactional
    public AdminUserResponse deactivateAdminUser(Long adminId, Long updatedBy) {
        log.info("Deactivating admin user: adminId={}, updatedBy={}", adminId, updatedBy);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin user not found: " + adminId));

        // Self-protection: Cannot deactivate self
        if (adminId.equals(updatedBy)) {
            throw new BusinessException("Cannot deactivate your own account");
        }

        if (!admin.getIsActive()) {
            log.debug("Admin user already inactive: adminId={}", adminId);
            return toResponse(admin);
        }

        admin.setIsActive(false);
        admin = adminUserRepository.save(admin);

        AdminUser updater = adminUserRepository.findById(updatedBy)
                .orElseThrow(() -> new BusinessException("Updater admin not found: " + updatedBy));

        // Log audit
        auditService.logAction(updatedBy, updater.getUsername(),
                AdminAuditLog.AdminActionType.ADMIN_USER_DEACTIVATED,
                "ADMIN_USER", adminId,
                String.format("Deactivated admin user: username=%s", admin.getUsername()));

        return toResponse(admin);
    }

    @Override
    @Transactional
    public AdminUserResponse unlockAdminUser(Long adminId, Long updatedBy) {
        log.info("Unlocking admin user: adminId={}, updatedBy={}", adminId, updatedBy);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin user not found: " + adminId));

        admin.setLockedUntil(null);
        admin.setFailedLoginAttempts(0);
        admin = adminUserRepository.save(admin);

        AdminUser updater = adminUserRepository.findById(updatedBy)
                .orElseThrow(() -> new BusinessException("Updater admin not found: " + updatedBy));

        // Log audit
        auditService.logAction(updatedBy, updater.getUsername(),
                AdminAuditLog.AdminActionType.ADMIN_USER_UNLOCKED,
                "ADMIN_USER", adminId,
                String.format("Unlocked admin user: username=%s", admin.getUsername()));

        return toResponse(admin);
    }

    @Override
    @Transactional
    public void resetFailedLoginAttempts(Long adminId, Long updatedBy) {
        log.info("Resetting failed login attempts: adminId={}, updatedBy={}", adminId, updatedBy);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin user not found: " + adminId));

        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        adminUserRepository.save(admin);

        AdminUser updater = adminUserRepository.findById(updatedBy)
                .orElseThrow(() -> new BusinessException("Updater admin not found: " + updatedBy));

        // Log audit
        auditService.logAction(updatedBy, updater.getUsername(),
                AdminAuditLog.AdminActionType.ADMIN_USER_LOGIN_ATTEMPTS_RESET,
                "ADMIN_USER", adminId,
                String.format("Reset failed login attempts for admin user: username=%s", admin.getUsername()));
    }

    @Override
    @Transactional
    public void changePassword(Long adminId, ChangePasswordRequest request) {
        log.info("Changing password: adminId={}", adminId);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin user not found: " + adminId));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Validate new password strength
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            String error = PasswordValidator.getValidationError(request.getNewPassword());
            throw new BusinessException(error != null ? error : "New password does not meet security requirements");
        }

        // Encode and set new password
        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.save(admin);

        // Log audit
        auditService.logAction(adminId, admin.getUsername(),
                AdminAuditLog.AdminActionType.ADMIN_USER_PASSWORD_CHANGED,
                "ADMIN_USER", adminId,
                "Changed own password");

        log.info("Password changed successfully: adminId={}", adminId);
    }

    @Override
    @Transactional
    public void resetPassword(Long adminId, ResetPasswordRequest request, Long resetBy) {
        log.info("Resetting password: adminId={}, resetBy={}", adminId, resetBy);

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin user not found: " + adminId));

        // Validate new password strength
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            String error = PasswordValidator.getValidationError(request.getNewPassword());
            throw new BusinessException(error != null ? error : "New password does not meet security requirements");
        }

        // Encode and set new password
        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        adminUserRepository.save(admin);

        AdminUser reseter = adminUserRepository.findById(resetBy)
                .orElseThrow(() -> new BusinessException("Resetter admin not found: " + resetBy));

        // Log audit
        auditService.logAction(resetBy, reseter.getUsername(),
                AdminAuditLog.AdminActionType.ADMIN_USER_PASSWORD_RESET,
                "ADMIN_USER", adminId,
                String.format("Reset password for admin user: username=%s", admin.getUsername()));

        log.info("Password reset successfully: adminId={}, resetBy={}", adminId, resetBy);
    }

    // ==================== Helper Methods ====================

    private AdminUserResponse toResponse(AdminUser admin) {
        AdminUserResponse.AdminUserResponseBuilder builder = AdminUserResponse.builder()
                .adminId(admin.getAdminId())
                .username(admin.getUsername())
                .email(admin.getEmail())
                .role(admin.getRole())
                .isActive(admin.getIsActive())
                .lastLoginAt(admin.getLastLoginAt())
                .failedLoginAttempts(admin.getFailedLoginAttempts())
                .lockedUntil(admin.getLockedUntil())
                .createdAt(admin.getCreatedAt())
                .updatedAt(admin.getUpdatedAt())
                .createdBy(admin.getCreatedBy());

        if (admin.getCreatedBy() != null) {
            String createdByUsername = getAdminUsername(admin.getCreatedBy());
            builder.createdByUsername(createdByUsername);
        }

        return builder.build();
    }

    private AdminUserSummaryResponse toSummaryResponse(AdminUser admin) {
        return AdminUserSummaryResponse.builder()
                .adminId(admin.getAdminId())
                .username(admin.getUsername())
                .email(admin.getEmail())
                .role(admin.getRole())
                .isActive(admin.getIsActive())
                .lastLoginAt(admin.getLastLoginAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }

    private String getAdminUsername(Long adminId) {
        return adminUserRepository.findById(adminId)
                .map(AdminUser::getUsername)
                .orElse("Unknown");
    }

    private int getRoleLevel(AdminUser.AdminRole role) {
        return switch (role) {
            case SUPER_ADMIN -> 4;
            case ADMIN -> 3;
            case MODERATOR -> 2;
            case VIEWER -> 1;
        };
    }

    private void validateRoleChange(AdminUser targetAdmin, AdminUser.AdminRole newRole,
                                   AdminUser updater, boolean isSelfUpdate) {
        int updaterLevel = getRoleLevel(updater.getRole());
        int targetLevel = getRoleLevel(targetAdmin.getRole());
        int newLevel = getRoleLevel(newRole);

        if (newLevel > updaterLevel) {
            throw new BusinessException("Cannot assign role higher than your own role");
        }

        if (targetLevel >= updaterLevel && !updater.getRole().equals(AdminUser.AdminRole.SUPER_ADMIN)) {
            throw new BusinessException("Cannot change role of admin with same or higher privilege level");
        }

        if (isSelfUpdate && newLevel < targetLevel) {
            throw new BusinessException("Cannot demote your own role");
        }

        if (isSelfUpdate && updater.getRole().equals(AdminUser.AdminRole.SUPER_ADMIN)) {
            throw new BusinessException("SUPER_ADMIN cannot change own role");
        }
    }
}

