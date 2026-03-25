package com.tuttime.admin.service;

import com.tuttime.admin.dto.AdminLoginResponse;
import com.tuttime.admin.entity.AdminUser;
import com.tuttime.admin.repository.AdminAuditLogRepository;
import com.tuttime.admin.repository.AdminUserRepository;
import com.tuttime.admin.util.AdminJwtTokenUtil;
import com.tuttime.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Admin Authentication Service: handles admin user authentication, login, logout, token management, brute-force protection, and account locking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

	private final AdminUserRepository adminUserRepository;
	private final AdminAuditLogRepository auditLogRepository;
	private final PasswordEncoder passwordEncoder;
	private final AdminJwtTokenUtil jwtTokenUtil;
	private final AdminTokenWhitelistService tokenWhitelistService;
	private final AdminTokenBlacklistService tokenBlacklistService;
	private final AdminAuditService auditService;

	private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
	private static final int LOCKOUT_DURATION_MINUTES = 30;

	/**
	 * Admin login.
	 */
	@Transactional
	public AdminLoginResponse login(String username, String password, String ipAddress, String userAgent) {
		AdminUser admin = adminUserRepository.findActiveByUsername(username)
				.orElseThrow(() -> {
					auditService.logLoginFailed(username, ipAddress, userAgent, "User not found");
					return new BusinessException("Invalid credentials");
				});

		if (admin.isLocked()) {
			auditService.logLoginFailed(username, ipAddress, userAgent, "Account locked");
			throw new BusinessException("Account is locked. Please try again later.");
		}

		if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
			handleFailedLogin(admin, ipAddress, userAgent);
			throw new BusinessException("Invalid credentials");
		}

		admin.setFailedLoginAttempts(0);
		admin.setLockedUntil(null);
		admin.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
		adminUserRepository.save(admin);

		String jti = jwtTokenUtil.generateJti();
		String token = jwtTokenUtil.generateToken(
				admin.getAdminId(),
				admin.getUsername(),
				admin.getRole().name(),
				jti
		);

		long expirationTime = System.currentTimeMillis() + jwtTokenUtil.getExpiration();
		tokenWhitelistService.addToWhitelist(jti, admin.getAdminId(), expirationTime);

		auditService.logLogin(admin.getAdminId(), admin.getUsername(), ipAddress, userAgent);

		log.info("Admin login successful: adminId={}, username={}", admin.getAdminId(), admin.getUsername());

		return AdminLoginResponse.builder()
				.token(token)
				.adminId(admin.getAdminId())
				.username(admin.getUsername())
				.role(admin.getRole().name())
				.build();
	}

	/**
	 * Handles failed login attempt.
	 */
	private void handleFailedLogin(AdminUser admin, String ipAddress, String userAgent) {
		int attempts = admin.getFailedLoginAttempts() + 1;
		admin.setFailedLoginAttempts(attempts);

		if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
			admin.setLockedUntil(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(LOCKOUT_DURATION_MINUTES));
			log.warn("Admin account locked due to failed login attempts: adminId={}, username={}",
					admin.getAdminId(), admin.getUsername());
		}

		adminUserRepository.save(admin);
		auditService.logLoginFailed(admin.getUsername(), ipAddress, userAgent,
				"Failed attempt " + attempts + "/" + MAX_FAILED_LOGIN_ATTEMPTS);
	}

	/**
	 * Admin logout
	 */
	@Transactional
	public void logout(String token, Long adminId) {
		String jti = jwtTokenUtil.getJtiFromToken(token);

		// Remove from whitelist
		tokenWhitelistService.removeFromWhitelist(jti);

		// Add to blacklist
		tokenBlacklistService.addToBlacklist(jti);

		// Log logout
		auditService.logLogout(adminId);

		log.info("Admin logout: adminId={}", adminId);
	}

	/**
	 * Validate admin token
	 */
	public boolean validateToken(String token) {
		try {
			// Extract JTI
			String jti = jwtTokenUtil.getJtiFromToken(token);
			if (jti == null) {
				return false;
			}

			// Check whitelist (MANDATORY - token must be whitelisted)
			if (!tokenWhitelistService.isTokenWhitelisted(jti)) {
				log.debug("Admin token not whitelisted: jti={}", jti);
				return false;
			}

			// Check blacklist
			if (tokenBlacklistService.isTokenBlacklisted(jti)) {
				log.debug("Admin token blacklisted: jti={}", jti);
				return false;
			}

			// Validate ISS and AUD (MANDATORY)
			if (!jwtTokenUtil.validateIssuer(token)) {
				log.debug("Admin token ISS validation failed");
				return false;
			}

			if (!jwtTokenUtil.validateAudience(token)) {
				log.debug("Admin token AUD validation failed");
				return false;
			}

			// Validate signature and expiration
			return jwtTokenUtil.validateTokenSignature(token);

		} catch (Exception e) {
			log.debug("Admin token validation failed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Get expiration time (for whitelist TTL)
	 */
	public Long getExpiration() {
		return jwtTokenUtil.getExpiration();
	}
}

