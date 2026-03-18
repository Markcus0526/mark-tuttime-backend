package com.tuttime.admin.service;

import com.tuttime.admin.dto.BlockedUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin User Service Interface
 * Provides user management functionality for admin panel
 */
public interface AdminUserService {
    Page<BlockedUserResponse> listBlockedUsers(Pageable pageable);
}

