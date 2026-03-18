package com.tuttime.admin.service;

import com.tuttime.admin.dto.TokenInfoResponse;
import com.tuttime.admin.dto.TokenRevocationResponse;

import java.util.List;

/**
 * Admin Token Management Service Interface
 */
public interface AdminTokenManagementService {
    TokenRevocationResponse revokeAllUserTokens(Long userId);
    void revokeToken(String jti);
    List<TokenInfoResponse> listUserTokens(Long userId);
}

