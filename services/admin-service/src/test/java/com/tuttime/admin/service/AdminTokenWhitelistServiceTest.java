package com.tuttime.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTokenWhitelistService Tests")
class AdminTokenWhitelistServiceTest {

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private SetOperations<String, String> setOperations;

	@Mock
	private ObjectMapper objectMapper;

	private AdminTokenWhitelistService tokenWhitelistService;

	private static final String JTI = "jti-123";
	private static final Long ADMIN_ID = 1L;
	private static final Long EXPIRATION_TIME = System.currentTimeMillis() + 3600000;

	@BeforeEach
	void setUp() {
		// Create service with mocked ObjectMapper and inject redisTemplate via reflection
		tokenWhitelistService = new AdminTokenWhitelistService(objectMapper);
		ReflectionTestUtils.setField(tokenWhitelistService, "redisTemplate", redisTemplate);
	}

	@Test
	@DisplayName("Should add token to whitelist successfully")
	void testAddToWhitelist_Success_ReturnsTrue() throws Exception {
		// Given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		String metadataJson = "{\"adminId\":1,\"issuedAt\":1234567890,\"expiresAt\":1234567890}";
		when(objectMapper.writeValueAsString(any())).thenReturn(metadataJson);
		when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

		// When
		boolean result = tokenWhitelistService.addToWhitelist(JTI, ADMIN_ID, EXPIRATION_TIME);

		// Then
		assertThat(result).isTrue();
		verify(valueOperations).set(anyString(), eq(metadataJson), anyLong(), eq(TimeUnit.MILLISECONDS));
		verify(setOperations).add(anyString(), eq(JTI));
		verify(redisTemplate).expire(anyString(), anyLong(), eq(TimeUnit.MILLISECONDS));
	}

	@Test
	@DisplayName("Should return false when Redis is not available")
	void testAddToWhitelist_RedisNotAvailable_ReturnsFalse() {
		// Given
		AdminTokenWhitelistService service = new AdminTokenWhitelistService(objectMapper);

		// When
		boolean result = service.addToWhitelist(JTI, ADMIN_ID, EXPIRATION_TIME);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Should return false when token is already expired")
	void testAddToWhitelist_ExpiredToken_ReturnsFalse() {
		// Given
		Long expiredTime = System.currentTimeMillis() - 1000;

		// When
		boolean result = tokenWhitelistService.addToWhitelist(JTI, ADMIN_ID, expiredTime);

		// Then
		assertThat(result).isFalse();
		verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
	}

	@Test
	@DisplayName("Should check if token is whitelisted")
	void testIsTokenWhitelisted_TokenExists_ReturnsTrue() {
		// Given
		when(redisTemplate.hasKey(anyString())).thenReturn(true);

		// When
		boolean result = tokenWhitelistService.isTokenWhitelisted(JTI);

		// Then
		assertThat(result).isTrue();
		verify(redisTemplate).hasKey(anyString());
	}

	@Test
	@DisplayName("Should return false when token is not whitelisted")
	void testIsTokenWhitelisted_TokenNotExists_ReturnsFalse() {
		// Given
		when(redisTemplate.hasKey(anyString())).thenReturn(false);

		// When
		boolean result = tokenWhitelistService.isTokenWhitelisted(JTI);

		// Then
		assertThat(result).isFalse();
		verify(redisTemplate).hasKey(anyString());
	}

	@Test
	@DisplayName("Should return true when Redis is not available")
	void testIsTokenWhitelisted_RedisNotAvailable_ReturnsTrue() {
		// Given
		AdminTokenWhitelistService service = new AdminTokenWhitelistService(objectMapper);

		// When
		boolean result = service.isTokenWhitelisted(JTI);

		// Then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("Should remove token from whitelist successfully")
	void testRemoveFromWhitelist_Success_ReturnsTrue() throws Exception {
		// Given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		AdminTokenWhitelistService.AdminTokenMetadata metadata =
				AdminTokenWhitelistService.AdminTokenMetadata.builder()
						.adminId(ADMIN_ID)
						.issuedAt(System.currentTimeMillis())
						.expiresAt(EXPIRATION_TIME)
						.build();
		String metadataJson = "{\"adminId\":1}";
		when(valueOperations.get(anyString())).thenReturn(metadataJson);
		when(objectMapper.readValue(anyString(), eq(AdminTokenWhitelistService.AdminTokenMetadata.class)))
				.thenReturn(metadata);

		// When
		boolean result = tokenWhitelistService.removeFromWhitelist(JTI);

		// Then
		assertThat(result).isTrue();
		verify(redisTemplate).delete(anyString());
		verify(setOperations).remove(anyString(), eq(JTI));
	}

	@Test
	@DisplayName("Should revoke all tokens for admin")
	void testRevokeAllAdminTokens_Success_ReturnsCount() throws Exception {
		// Given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		Set<String> jtiSet = new HashSet<>();
		jtiSet.add("jti-1");
		jtiSet.add("jti-2");
		when(setOperations.members(anyString())).thenReturn(jtiSet);
		when(valueOperations.get(anyString())).thenReturn("{\"adminId\":1,\"issuedAt\":1234567890,\"expiresAt\":1234567890}");
		when(objectMapper.readValue(anyString(), eq(AdminTokenWhitelistService.AdminTokenMetadata.class)))
				.thenReturn(AdminTokenWhitelistService.AdminTokenMetadata.builder()
						.adminId(ADMIN_ID)
						.issuedAt(System.currentTimeMillis())
						.expiresAt(EXPIRATION_TIME)
						.build());
		when(redisTemplate.delete(anyString())).thenReturn(true);

		// When
		int count = tokenWhitelistService.revokeAllAdminTokens(ADMIN_ID);

		// Then
		assertThat(count).isGreaterThanOrEqualTo(0);
		verify(redisTemplate, atLeastOnce()).delete(anyString());
	}

	@Test
	@DisplayName("Should return 0 when no tokens exist")
	void testRevokeAllAdminTokens_NoTokens_ReturnsZero() {
		// Given
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(setOperations.members(anyString())).thenReturn(new HashSet<>());

		// When
		int count = tokenWhitelistService.revokeAllAdminTokens(ADMIN_ID);

		// Then
		assertThat(count).isEqualTo(0);
		verify(redisTemplate, never()).delete(anyString());
	}
}

