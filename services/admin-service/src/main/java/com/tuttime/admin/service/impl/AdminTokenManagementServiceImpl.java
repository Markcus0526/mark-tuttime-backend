package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.TokenInfoResponse;
import com.tuttime.admin.dto.TokenRevocationResponse;
import com.tuttime.admin.service.AdminTokenBlacklistService;
import com.tuttime.admin.service.AdminTokenManagementService;
import com.tuttime.admin.service.AdminTokenWhitelistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin Token Management Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTokenManagementServiceImpl implements AdminTokenManagementService {

	private final AdminTokenWhitelistService tokenWhitelistService;
	private final AdminTokenBlacklistService tokenBlacklistService;

	@Override
	public TokenRevocationResponse revokeAllUserTokens(Long userId) {
		try {
			int revokedCount = tokenWhitelistService.revokeAllAdminTokens(userId);
			// Note: For regular user tokens, we need to call user-service's token revocation
			// This is a placeholder - actual implementation should call user-service

			return TokenRevocationResponse.builder()
					.userId(userId)
					.revokedCount(revokedCount)
					.build();
		} catch (Exception e) {
			log.error("Error revoking all user tokens: userId={}", userId, e);
			throw new RuntimeException("Failed to revoke user tokens: " + e.getMessage(), e);
		}
	}

	@Override
	public void revokeToken(String jti) {
		try {
			// Remove from whitelist
			tokenWhitelistService.removeFromWhitelist(jti);
			// Add to blacklist
			tokenBlacklistService.addToBlacklist(jti);
		} catch (Exception e) {
			log.error("Error revoking token: jti={}", jti, e);
			throw new RuntimeException("Failed to revoke token: " + e.getMessage(), e);
		}
	}

	@Override
	public List<TokenInfoResponse> listUserTokens(Long userId) {
		// TODO: Implement token listing - requires access to token metadata
		log.info("Listing user tokens: userId={}", userId);
		return new ArrayList<>();
	}
}

