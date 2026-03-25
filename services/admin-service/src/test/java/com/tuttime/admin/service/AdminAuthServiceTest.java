package com.tuttime.admin.service;

import com.tuttime.admin.dto.AdminLoginResponse;
import com.tuttime.admin.entity.AdminUser;
import com.tuttime.admin.repository.AdminAuditLogRepository;
import com.tuttime.admin.repository.AdminUserRepository;
import com.tuttime.admin.util.AdminJwtTokenUtil;
import com.tuttime.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthService Tests")
class AdminAuthServiceTest {

	@Mock
	private AdminUserRepository adminUserRepository;

	@Mock
	private AdminAuditLogRepository auditLogRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AdminJwtTokenUtil jwtTokenUtil;

	@Mock
	private AdminTokenWhitelistService tokenWhitelistService;

	@Mock
	private AdminTokenBlacklistService tokenBlacklistService;

	@Mock
	private AdminAuditService auditService;

	@InjectMocks
	private AdminAuthService adminAuthService;

	private AdminUser adminUser;
	private static final String USERNAME = "admin";
	private static final String PASSWORD = "password123";
	private static final String HASHED_PASSWORD = "$2a$10$hashedpassword";
	private static final String IP_ADDRESS = "192.168.1.1";
	private static final String USER_AGENT = "Mozilla/5.0";

	@BeforeEach
	void setUp() {
		adminUser = AdminUser.builder()
				.adminId(1L)
				.username(USERNAME)
				.email("admin@example.com")
				.passwordHash(HASHED_PASSWORD)
				.role(AdminUser.AdminRole.ADMIN)
				.isActive(true)
				.failedLoginAttempts(0)
				.lockedUntil(null)
				.build();
	}

	@Test
	@DisplayName("Should login successfully with valid credentials")
	void testLogin_ValidCredentials_ReturnsLoginResponse() {
		// Given
		String jti = "jti-123";
		String token = "jwt-token";
		long expirationTime = System.currentTimeMillis() + 3600000;

		when(adminUserRepository.findActiveByUsername(USERNAME))
				.thenReturn(Optional.of(adminUser));
		when(passwordEncoder.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
		when(jwtTokenUtil.generateJti()).thenReturn(jti);
		when(jwtTokenUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn(token);
		when(jwtTokenUtil.getExpiration()).thenReturn(3600000L);
		when(tokenWhitelistService.addToWhitelist(anyString(), anyLong(), anyLong())).thenReturn(true);

		// When
		AdminLoginResponse response = adminAuthService.login(USERNAME, PASSWORD, IP_ADDRESS, USER_AGENT);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getToken()).isEqualTo(token);
		assertThat(response.getAdminId()).isEqualTo(1L);
		assertThat(response.getUsername()).isEqualTo(USERNAME);
		assertThat(response.getRole()).isEqualTo("ADMIN");

		verify(adminUserRepository).save(adminUser);
		verify(tokenWhitelistService).addToWhitelist(eq(jti), eq(1L), anyLong());
		verify(auditService).logLogin(1L, USERNAME, IP_ADDRESS, USER_AGENT);
		assertThat(adminUser.getFailedLoginAttempts()).isEqualTo(0);
		assertThat(adminUser.getLockedUntil()).isNull();
	}

	@Test
	@DisplayName("Should throw exception when user not found")
	void testLogin_UserNotFound_ThrowsException() {
		// Given
		when(adminUserRepository.findActiveByUsername(USERNAME))
				.thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> adminAuthService.login(USERNAME, PASSWORD, IP_ADDRESS, USER_AGENT))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Invalid credentials");

		verify(auditService).logLoginFailed(USERNAME, IP_ADDRESS, USER_AGENT, "User not found");
		verify(adminUserRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should throw exception when account is locked")
	void testLogin_AccountLocked_ThrowsException() {
		// Given
		adminUser.setLockedUntil(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));
		when(adminUserRepository.findActiveByUsername(USERNAME))
				.thenReturn(Optional.of(adminUser));

		// When/Then
		assertThatThrownBy(() -> adminAuthService.login(USERNAME, PASSWORD, IP_ADDRESS, USER_AGENT))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Account is locked");

		verify(auditService).logLoginFailed(USERNAME, IP_ADDRESS, USER_AGENT, "Account locked");
		verify(adminUserRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should throw exception when password is incorrect")
	void testLogin_InvalidPassword_ThrowsException() {
		// Given
		when(adminUserRepository.findActiveByUsername(USERNAME))
				.thenReturn(Optional.of(adminUser));
		when(passwordEncoder.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> adminAuthService.login(USERNAME, PASSWORD, IP_ADDRESS, USER_AGENT))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Invalid credentials");

		verify(adminUserRepository).save(adminUser);
		verify(auditService).logLoginFailed(eq(USERNAME), eq(IP_ADDRESS), eq(USER_AGENT), anyString());
		assertThat(adminUser.getFailedLoginAttempts()).isEqualTo(1);
	}

	@Test
	@DisplayName("Should lock account after max failed attempts")
	void testLogin_MaxFailedAttempts_LocksAccount() {
		// Given
		adminUser.setFailedLoginAttempts(4);
		when(adminUserRepository.findActiveByUsername(USERNAME))
				.thenReturn(Optional.of(adminUser));
		when(passwordEncoder.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> adminAuthService.login(USERNAME, PASSWORD, IP_ADDRESS, USER_AGENT))
				.isInstanceOf(BusinessException.class);

		verify(adminUserRepository).save(adminUser);
		assertThat(adminUser.getFailedLoginAttempts()).isEqualTo(5);
		assertThat(adminUser.getLockedUntil()).isNotNull();
	}

	@Test
	@DisplayName("Should logout successfully")
	void testLogout_Success() {
		// Given
		String token = "jwt-token";
		String jti = "jti-123";
		Long adminId = 1L;

		when(jwtTokenUtil.getJtiFromToken(token)).thenReturn(jti);
		when(tokenWhitelistService.removeFromWhitelist(jti)).thenReturn(true);

		// When
		adminAuthService.logout(token, adminId);

		// Then
		verify(tokenWhitelistService).removeFromWhitelist(jti);
		verify(tokenBlacklistService).addToBlacklist(jti);
		verify(auditService).logLogout(adminId);
	}

	@Test
	@DisplayName("Should validate token successfully")
	void testValidateToken_ValidToken_ReturnsTrue() {
		// Given
		String token = "jwt-token";
		String jti = "jti-123";

		when(jwtTokenUtil.getJtiFromToken(token)).thenReturn(jti);
		when(tokenWhitelistService.isTokenWhitelisted(jti)).thenReturn(true);
		when(tokenBlacklistService.isTokenBlacklisted(jti)).thenReturn(false);
		when(jwtTokenUtil.validateIssuer(token)).thenReturn(true);
		when(jwtTokenUtil.validateAudience(token)).thenReturn(true);
		when(jwtTokenUtil.validateTokenSignature(token)).thenReturn(true);

		// When
		boolean isValid = adminAuthService.validateToken(token);

		// Then
		assertThat(isValid).isTrue();
	}

	@Test
	@DisplayName("Should return false when token is not whitelisted")
	void testValidateToken_NotWhitelisted_ReturnsFalse() {
		// Given
		String token = "jwt-token";
		String jti = "jti-123";

		when(jwtTokenUtil.getJtiFromToken(token)).thenReturn(jti);
		when(tokenWhitelistService.isTokenWhitelisted(jti)).thenReturn(false);

		// When
		boolean isValid = adminAuthService.validateToken(token);

		// Then
		assertThat(isValid).isFalse();
		verify(tokenBlacklistService, never()).isTokenBlacklisted(anyString());
	}

	@Test
	@DisplayName("Should return false when token is blacklisted")
	void testValidateToken_Blacklisted_ReturnsFalse() {
		// Given
		String token = "jwt-token";
		String jti = "jti-123";

		when(jwtTokenUtil.getJtiFromToken(token)).thenReturn(jti);
		when(tokenWhitelistService.isTokenWhitelisted(jti)).thenReturn(true);
		when(tokenBlacklistService.isTokenBlacklisted(jti)).thenReturn(true);

		// When
		boolean isValid = adminAuthService.validateToken(token);

		// Then
		assertThat(isValid).isFalse();
		verify(jwtTokenUtil, never()).validateIssuer(anyString());
	}

	@Test
	@DisplayName("Should return false when issuer validation fails")
	void testValidateToken_InvalidIssuer_ReturnsFalse() {
		// Given
		String token = "jwt-token";
		String jti = "jti-123";

		when(jwtTokenUtil.getJtiFromToken(token)).thenReturn(jti);
		when(tokenWhitelistService.isTokenWhitelisted(jti)).thenReturn(true);
		when(tokenBlacklistService.isTokenBlacklisted(jti)).thenReturn(false);
		when(jwtTokenUtil.validateIssuer(token)).thenReturn(false);

		// When
		boolean isValid = adminAuthService.validateToken(token);

		// Then
		assertThat(isValid).isFalse();
		verify(jwtTokenUtil, never()).validateTokenSignature(anyString());
	}

	@Test
	@DisplayName("Should return false when audience validation fails")
	void testValidateToken_InvalidAudience_ReturnsFalse() {
		// Given
		String token = "jwt-token";
		String jti = "jti-123";

		when(jwtTokenUtil.getJtiFromToken(token)).thenReturn(jti);
		when(tokenWhitelistService.isTokenWhitelisted(jti)).thenReturn(true);
		when(tokenBlacklistService.isTokenBlacklisted(jti)).thenReturn(false);
		when(jwtTokenUtil.validateIssuer(token)).thenReturn(true);
		when(jwtTokenUtil.validateAudience(token)).thenReturn(false);

		// When
		boolean isValid = adminAuthService.validateToken(token);

		// Then
		assertThat(isValid).isFalse();
		verify(jwtTokenUtil, never()).validateTokenSignature(anyString());
	}

	@Test
	@DisplayName("Should return false when token signature validation fails")
	void testValidateToken_InvalidSignature_ReturnsFalse() {
		// Given
		String token = "jwt-token";
		String jti = "jti-123";

		when(jwtTokenUtil.getJtiFromToken(token)).thenReturn(jti);
		when(tokenWhitelistService.isTokenWhitelisted(jti)).thenReturn(true);
		when(tokenBlacklistService.isTokenBlacklisted(jti)).thenReturn(false);
		when(jwtTokenUtil.validateIssuer(token)).thenReturn(true);
		when(jwtTokenUtil.validateAudience(token)).thenReturn(true);
		when(jwtTokenUtil.validateTokenSignature(token)).thenReturn(false);

		// When
		boolean isValid = adminAuthService.validateToken(token);

		// Then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("Should return false when JTI is null")
	void testValidateToken_NullJti_ReturnsFalse() {
		// Given
		String token = "jwt-token";

		when(jwtTokenUtil.getJtiFromToken(token)).thenReturn(null);

		// When
		boolean isValid = adminAuthService.validateToken(token);

		// Then
		assertThat(isValid).isFalse();
		verify(tokenWhitelistService, never()).isTokenWhitelisted(anyString());
	}

	@Test
	@DisplayName("Should return false when exception occurs")
	void testValidateToken_Exception_ReturnsFalse() {
		// Given
		String token = "jwt-token";

		when(jwtTokenUtil.getJtiFromToken(token)).thenThrow(new RuntimeException("Error"));

		// When
		boolean isValid = adminAuthService.validateToken(token);

		// Then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("Should get expiration time")
	void testGetExpiration_ReturnsExpiration() {
		// Given
		Long expectedExpiration = 3600000L;
		when(jwtTokenUtil.getExpiration()).thenReturn(expectedExpiration);

		// When
		Long expiration = adminAuthService.getExpiration();

		// Then
		assertThat(expiration).isEqualTo(expectedExpiration);
	}
}

