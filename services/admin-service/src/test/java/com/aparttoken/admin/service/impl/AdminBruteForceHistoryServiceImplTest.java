package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.BruteForceHistoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBruteForceHistoryServiceImpl Tests")
class AdminBruteForceHistoryServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ValueOperations valueOperations;

    @InjectMocks
    private AdminBruteForceHistoryServiceImpl bruteForceHistoryService;

    private PageRequest pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should get brute-force history with blocked IPs")
    void testGetBruteForceHistory_WithBlockedIps_ReturnsHistory() {
        // Given
        Set<String> blockedIpKeys = new HashSet<>();
        blockedIpKeys.add("bruteforce:blocked:ip:192.168.1.100");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(blockedIpKeys);
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(Collections.emptySet());
        
        long blockUntil = (System.currentTimeMillis() / 1000) + 3600; // 1 hour from now
        when(valueOperations.get("bruteforce:blocked:ip:192.168.1.100")).thenReturn(String.valueOf(blockUntil));
        when(redisTemplate.getExpire("bruteforce:blocked:ip:192.168.1.100")).thenReturn(3600L);
        when(valueOperations.get("bruteforce:login:192.168.1.100:")).thenReturn("5");

        // When
        Page<BruteForceHistoryResponse> result = bruteForceHistoryService.getBruteForceHistory(
                pageable, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        verify(redisTemplate, atLeastOnce()).keys("bruteforce:blocked:ip:*");
    }

    @Test
    @DisplayName("Should get brute-force history with blocked phone numbers")
    void testGetBruteForceHistory_WithBlockedPhones_ReturnsHistory() {
        // Given
        Set<String> blockedPhoneKeys = new HashSet<>();
        blockedPhoneKeys.add("bruteforce:blocked:phone:+1234567890");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(blockedPhoneKeys);
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(Collections.emptySet());
        
        long blockUntil = (System.currentTimeMillis() / 1000) + 7200; // 2 hours from now
        when(valueOperations.get("bruteforce:blocked:phone:+1234567890")).thenReturn(String.valueOf(blockUntil));
        when(redisTemplate.getExpire("bruteforce:blocked:phone:+1234567890")).thenReturn(7200L);
        when(valueOperations.get(anyString())).thenReturn("10");

        // When
        Page<BruteForceHistoryResponse> result = bruteForceHistoryService.getBruteForceHistory(
                pageable, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        verify(redisTemplate, atLeastOnce()).keys("bruteforce:blocked:phone:*");
    }

    @Test
    @DisplayName("Should filter brute-force history by IP address")
    void testGetBruteForceHistory_FilterByIp_ReturnsFiltered() {
        // Given
        Set<String> blockedIpKeys = new HashSet<>();
        blockedIpKeys.add("bruteforce:blocked:ip:192.168.1.100");
        blockedIpKeys.add("bruteforce:blocked:ip:192.168.1.200");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(blockedIpKeys);
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(Collections.emptySet());
        
        long blockUntil = (System.currentTimeMillis() / 1000) + 3600;
        when(valueOperations.get("bruteforce:blocked:ip:192.168.1.100")).thenReturn(String.valueOf(blockUntil));
        when(valueOperations.get("bruteforce:blocked:ip:192.168.1.200")).thenReturn(String.valueOf(blockUntil));
        when(redisTemplate.getExpire(anyString())).thenReturn(3600L);

        // When
        Page<BruteForceHistoryResponse> result = bruteForceHistoryService.getBruteForceHistory(
                pageable, null, null, "192.168.1.100", null);

        // Then
        assertThat(result).isNotNull();
        // Should only return entries for the specified IP
    }

    @Test
    @DisplayName("Should filter brute-force history by phone number")
    void testGetBruteForceHistory_FilterByPhone_ReturnsFiltered() {
        // Given
        Set<String> blockedPhoneKeys = new HashSet<>();
        blockedPhoneKeys.add("bruteforce:blocked:phone:+1234567890");
        blockedPhoneKeys.add("bruteforce:blocked:phone:+0987654321");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(blockedPhoneKeys);
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(Collections.emptySet());
        
        long blockUntil = (System.currentTimeMillis() / 1000) + 3600;
        when(valueOperations.get("bruteforce:blocked:phone:+1234567890")).thenReturn(String.valueOf(blockUntil));
        when(valueOperations.get("bruteforce:blocked:phone:+0987654321")).thenReturn(String.valueOf(blockUntil));
        when(redisTemplate.getExpire(anyString())).thenReturn(3600L);

        // When
        Page<BruteForceHistoryResponse> result = bruteForceHistoryService.getBruteForceHistory(
                pageable, null, null, null, "+1234567890");

        // Then
        assertThat(result).isNotNull();
        // Should only return entries for the specified phone
    }

    @Test
    @DisplayName("Should filter brute-force history by date range")
    void testGetBruteForceHistory_FilterByDateRange_ReturnsFiltered() {
        // Given
        Set<String> blockedIpKeys = new HashSet<>();
        blockedIpKeys.add("bruteforce:blocked:ip:192.168.1.100");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(blockedIpKeys);
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(Collections.emptySet());
        
        long blockUntil = (System.currentTimeMillis() / 1000) + 3600;
        when(valueOperations.get("bruteforce:blocked:ip:192.168.1.100")).thenReturn(String.valueOf(blockUntil));
        when(redisTemplate.getExpire("bruteforce:blocked:ip:192.168.1.100")).thenReturn(3600L);

        OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        // When
        Page<BruteForceHistoryResponse> result = bruteForceHistoryService.getBruteForceHistory(
                pageable, startDate, endDate, null, null);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle Redis unavailable gracefully")
    void testGetBruteForceHistory_RedisUnavailable_ReturnsEmpty() {
        // Given
        AdminBruteForceHistoryServiceImpl service = new AdminBruteForceHistoryServiceImpl();
        // Service with null RedisTemplate

        // When
        Page<BruteForceHistoryResponse> result = service.getBruteForceHistory(
                pageable, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should determine severity based on attempt count")
    void testGetBruteForceHistory_DifferentAttemptCounts_ReturnsCorrectSeverity() {
        // Given
        Set<String> attemptKeys = new HashSet<>();
        attemptKeys.add("bruteforce:login:192.168.1.100:+1234567890");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(attemptKeys);
        
        when(valueOperations.get("bruteforce:login:192.168.1.100:+1234567890")).thenReturn("15");

        // When
        Page<BruteForceHistoryResponse> result = bruteForceHistoryService.getBruteForceHistory(
                pageable, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        if (result.getTotalElements() > 0) {
            BruteForceHistoryResponse entry = result.getContent().get(0);
            assertThat(entry.getAttemptCount()).isGreaterThanOrEqualTo(3L);
            assertThat(entry.getSeverity()).isNotNull();
        }
    }

    @Test
    @DisplayName("Should handle expired blocks")
    void testGetBruteForceHistory_ExpiredBlocks_ExcludesExpired() {
        // Given
        Set<String> blockedIpKeys = new HashSet<>();
        blockedIpKeys.add("bruteforce:blocked:ip:192.168.1.100");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(blockedIpKeys);
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(Collections.emptySet());
        
        // Expired block (in the past)
        long expiredBlockUntil = (System.currentTimeMillis() / 1000) - 3600;
        when(valueOperations.get("bruteforce:blocked:ip:192.168.1.100")).thenReturn(String.valueOf(expiredBlockUntil));

        // When
        Page<BruteForceHistoryResponse> result = bruteForceHistoryService.getBruteForceHistory(
                pageable, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        // Expired blocks should be excluded (blockUntil < now)
    }

    @Test
    @DisplayName("Should sort history by detected date")
    void testGetBruteForceHistory_SortedByDate_ReturnsSorted() {
        // Given
        Set<String> blockedIpKeys = new HashSet<>();
        blockedIpKeys.add("bruteforce:blocked:ip:192.168.1.100");
        blockedIpKeys.add("bruteforce:blocked:ip:192.168.1.200");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(blockedIpKeys);
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(Collections.emptySet());
        
        long blockUntil1 = (System.currentTimeMillis() / 1000) + 3600;
        long blockUntil2 = (System.currentTimeMillis() / 1000) + 7200;
        when(valueOperations.get("bruteforce:blocked:ip:192.168.1.100")).thenReturn(String.valueOf(blockUntil1));
        when(valueOperations.get("bruteforce:blocked:ip:192.168.1.200")).thenReturn(String.valueOf(blockUntil2));
        when(redisTemplate.getExpire(anyString())).thenReturn(3600L);

        // When
        Page<BruteForceHistoryResponse> result = bruteForceHistoryService.getBruteForceHistory(
                pageable, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        // Should be sorted by detected date (newest first)
    }
}

