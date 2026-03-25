package com.tuttime.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTokenBlacklistService Tests")
class AdminTokenBlacklistServiceTest {

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private AdminTokenBlacklistService tokenBlacklistService;

	private static final String JTI = "jti-123";

	@BeforeEach
	void setUp() {
		// Only set up when needed
		ReflectionTestUtils.setField(tokenBlacklistService, "tokenExpiration", 3600000L);
	}

	@Test
	@DisplayName("Should add token to blacklist successfully")
	void testAddToBlacklist_Success() {
		// Given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		// When
		tokenBlacklistService.addToBlacklist(JTI);

		// Then
		verify(valueOperations).set(anyString(), eq("revoked"), anyLong(), eq(TimeUnit.MILLISECONDS));
	}

	@Test
	@DisplayName("Should handle blacklist when Redis is not available")
	void testAddToBlacklist_RedisNotAvailable_NoException() {
		// Given
		AdminTokenBlacklistService service = new AdminTokenBlacklistService();
		ReflectionTestUtils.setField(service, "redisTemplate", null);
		ReflectionTestUtils.setField(service, "tokenExpiration", 3600000L);

		// When/Then - Should not throw exception
		service.addToBlacklist(JTI);
	}

	@Test
	@DisplayName("Should check if token is blacklisted")
	void testIsTokenBlacklisted_TokenExists_ReturnsTrue() {
		// Given
		when(redisTemplate.hasKey(anyString())).thenReturn(true);

		// When
		boolean result = tokenBlacklistService.isTokenBlacklisted(JTI);

		// Then
		assertThat(result).isTrue();
		verify(redisTemplate).hasKey(anyString());
	}

	@Test
	@DisplayName("Should return false when token is not blacklisted")
	void testIsTokenBlacklisted_TokenNotExists_ReturnsFalse() {
		// Given
		when(redisTemplate.hasKey(anyString())).thenReturn(false);

		// When
		boolean result = tokenBlacklistService.isTokenBlacklisted(JTI);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Should return false when Redis is not available")
	void testIsTokenBlacklisted_RedisNotAvailable_ReturnsFalse() {
		// Given
		AdminTokenBlacklistService service = new AdminTokenBlacklistService();
		ReflectionTestUtils.setField(service, "redisTemplate", null);

		// When
		boolean result = service.isTokenBlacklisted(JTI);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Should remove token from blacklist successfully")
	void testRemoveFromBlacklist_Success() {
		// Given - No need to stub valueOperations as it's not used in removeFromBlacklist

		// When
		tokenBlacklistService.removeFromBlacklist(JTI);

		// Then
		verify(redisTemplate).delete(anyString());
	}

	@Test
	@DisplayName("Should handle remove when Redis is not available")
	void testRemoveFromBlacklist_RedisNotAvailable_NoException() {
		// Given
		AdminTokenBlacklistService service = new AdminTokenBlacklistService();
		ReflectionTestUtils.setField(service, "redisTemplate", null);

		// When/Then - Should not throw exception
		service.removeFromBlacklist(JTI);
	}
}

