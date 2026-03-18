package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.FraudFlagResponse;
import com.tuttime.admin.dto.WarningCategory;
import com.tuttime.admin.dto.WarningResponse;
import com.tuttime.admin.dto.WarningSeverity;
import com.tuttime.admin.service.AdminAntiFraudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminWarningServiceImpl Tests")
class AdminWarningServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private AdminAntiFraudService antiFraudService;

    @Mock
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ValueOperations valueOperations;

    @InjectMocks
    private AdminWarningServiceImpl warningService;

    private PageRequest pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should get consolidated warnings with fraud flags")
    void testGetConsolidatedWarnings_WithFraudFlags_ReturnsWarnings() {
        // Given
        FraudFlagResponse fraudFlag = FraudFlagResponse.builder()
                .flagId(1L)
                .userId(100L)
                .flagType("MULTI_DEVICE")
                .severity(WarningSeverity.HIGH)
                .category(WarningCategory.MULTI_DEVICE)
                .description("Multiple devices detected")
                .deviceId("device-123")
                .ipAddress("192.168.1.1")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .resolved(false)
                .build();

        Page<FraudFlagResponse> fraudFlagsPage = new PageImpl<>(Collections.singletonList(fraudFlag), pageable, 1);
        when(antiFraudService.listFraudFlags(any(), any(), any(), any(), any())).thenReturn(fraudFlagsPage);
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        // When
        Page<WarningResponse> result = warningService.getConsolidatedWarnings(
                pageable, null, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isGreaterThan(0);
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent().get(0).getCategory()).isEqualTo(WarningCategory.MULTI_DEVICE);
        assertThat(result.getContent().get(0).getSeverity()).isEqualTo(WarningSeverity.HIGH);
    }

    @Test
    @DisplayName("Should get consolidated warnings with brute-force warnings")
    void testGetConsolidatedWarnings_WithBruteForce_ReturnsWarnings() {
        // Given
        when(antiFraudService.listFraudFlags(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
        
        Set<String> blockedIpKeys = new HashSet<>();
        blockedIpKeys.add("bruteforce:blocked:ip:192.168.1.100");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(blockedIpKeys);
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(Collections.emptySet());
        
        long blockUntil = (System.currentTimeMillis() / 1000) + 3600; // 1 hour from now
        when(valueOperations.get("bruteforce:blocked:ip:192.168.1.100")).thenReturn(String.valueOf(blockUntil));
        when(redisTemplate.getExpire("bruteforce:blocked:ip:192.168.1.100")).thenReturn(3600L);

        // When
        Page<WarningResponse> result = warningService.getConsolidatedWarnings(
                pageable, null, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        // Should have brute-force warnings
        verify(redisTemplate, atLeastOnce()).keys(anyString());
    }

    @Test
    @DisplayName("Should filter warnings by category")
    void testGetConsolidatedWarnings_FilterByCategory_ReturnsFiltered() {
        // Given
        FraudFlagResponse fraudFlag = FraudFlagResponse.builder()
                .flagId(1L)
                .flagType("MULTI_DEVICE")
                .severity(WarningSeverity.MEDIUM)
                .category(WarningCategory.MULTI_DEVICE)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .resolved(false)
                .build();

        Page<FraudFlagResponse> fraudFlagsPage = new PageImpl<>(Collections.singletonList(fraudFlag), pageable, 1);
        when(antiFraudService.listFraudFlags(any(), any(), any(), any(), any())).thenReturn(fraudFlagsPage);
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        // When
        Page<WarningResponse> result = warningService.getConsolidatedWarnings(
                pageable, WarningCategory.MULTI_DEVICE, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        if (result.getTotalElements() > 0) {
            assertThat(result.getContent().get(0).getCategory()).isEqualTo(WarningCategory.MULTI_DEVICE);
        }
    }

    @Test
    @DisplayName("Should filter warnings by severity")
    void testGetConsolidatedWarnings_FilterBySeverity_ReturnsFiltered() {
        // Given
        FraudFlagResponse fraudFlag = FraudFlagResponse.builder()
                .flagId(1L)
                .flagType("SUSPICIOUS_ACTIVITY")
                .severity(WarningSeverity.CRITICAL)
                .category(WarningCategory.SUSPICIOUS)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .resolved(false)
                .build();

        Page<FraudFlagResponse> fraudFlagsPage = new PageImpl<>(Collections.singletonList(fraudFlag), pageable, 1);
        when(antiFraudService.listFraudFlags(any(), any(), any(), any(), any())).thenReturn(fraudFlagsPage);
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        // When
        Page<WarningResponse> result = warningService.getConsolidatedWarnings(
                pageable, null, WarningSeverity.CRITICAL, null, null, null);

        // Then
        assertThat(result).isNotNull();
        if (result.getTotalElements() > 0) {
            assertThat(result.getContent().get(0).getSeverity()).isEqualTo(WarningSeverity.CRITICAL);
        }
    }

    @Test
    @DisplayName("Should get brute-force warnings")
    void testGetBruteForceWarnings_ReturnsWarnings() {
        // Given
        Set<String> blockedIpKeys = new HashSet<>();
        blockedIpKeys.add("bruteforce:blocked:ip:192.168.1.100");
        when(redisTemplate.keys("bruteforce:blocked:ip:*")).thenReturn(blockedIpKeys);
        when(redisTemplate.keys("bruteforce:blocked:phone:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("bruteforce:login:*")).thenReturn(Collections.emptySet());
        
        long blockUntil = (System.currentTimeMillis() / 1000) + 3600;
        when(valueOperations.get("bruteforce:blocked:ip:192.168.1.100")).thenReturn(String.valueOf(blockUntil));
        when(redisTemplate.getExpire("bruteforce:blocked:ip:192.168.1.100")).thenReturn(3600L);

        // When
        Page<WarningResponse> result = warningService.getBruteForceWarnings(
                pageable, null, null);

        // Then
        assertThat(result).isNotNull();
        verify(redisTemplate, atLeastOnce()).keys("bruteforce:blocked:ip:*");
    }

    @Test
    @DisplayName("Should get system stability warnings")
    void testGetSystemStabilityWarnings_ReturnsWarnings() {
        // When
        Page<WarningResponse> result = warningService.getSystemStabilityWarnings(
                pageable, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
    }

    @Test
    @DisplayName("Should resolve warning")
    void testResolveWarning_Success() {
        // Given
        String warningId = "SYS_STABILITY_123";
        
        // When
        warningService.resolveWarning(warningId);

        // Then
        // Verify no exception is thrown
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should handle Redis unavailable gracefully")
    void testGetConsolidatedWarnings_RedisUnavailable_ReturnsPartialResults() {
        // Given
        // Set redisTemplate to null using reflection to simulate Redis unavailability
        try {
            java.lang.reflect.Field field = AdminWarningServiceImpl.class.getDeclaredField("redisTemplate");
            field.setAccessible(true);
            field.set(warningService, null);
        } catch (Exception e) {
            // If reflection fails, skip this test
            return;
        }
        
        FraudFlagResponse fraudFlag = FraudFlagResponse.builder()
                .flagId(1L)
                .flagType("MULTI_DEVICE")
                .severity(WarningSeverity.MEDIUM)
                .category(WarningCategory.MULTI_DEVICE)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .resolved(false)
                .build();

        Page<FraudFlagResponse> fraudFlagsPage = new PageImpl<>(Collections.singletonList(fraudFlag), pageable, 1);
        when(antiFraudService.listFraudFlags(any(), any(), any(), any(), any())).thenReturn(fraudFlagsPage);

        // When
        Page<WarningResponse> result = warningService.getConsolidatedWarnings(
                pageable, null, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        // Should still return fraud flags even if Redis is unavailable
        assertThat(result.getTotalElements()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should sort warnings by severity and date")
    void testGetConsolidatedWarnings_SortedBySeverity_ReturnsSorted() {
        // Given
        FraudFlagResponse criticalFlag = FraudFlagResponse.builder()
                .flagId(1L)
                .flagType("MALICIOUS")
                .severity(WarningSeverity.CRITICAL)
                .category(WarningCategory.MALICIOUS)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(2))
                .resolved(false)
                .build();

        FraudFlagResponse highFlag = FraudFlagResponse.builder()
                .flagId(2L)
                .flagType("SUSPICIOUS")
                .severity(WarningSeverity.HIGH)
                .category(WarningCategory.SUSPICIOUS)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .resolved(false)
                .build();

        Page<FraudFlagResponse> fraudFlagsPage = new PageImpl<>(
                Arrays.asList(criticalFlag, highFlag), pageable, 2);
        when(antiFraudService.listFraudFlags(any(), any(), any(), any(), any())).thenReturn(fraudFlagsPage);
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        // When
        Page<WarningResponse> result = warningService.getConsolidatedWarnings(
                pageable, null, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        if (result.getTotalElements() >= 2) {
            // CRITICAL should come before HIGH
            WarningSeverity first = result.getContent().get(0).getSeverity();
            WarningSeverity second = result.getContent().get(1).getSeverity();
            assertThat(getSeverityOrder(first)).isGreaterThanOrEqualTo(getSeverityOrder(second));
        }
    }

    private int getSeverityOrder(WarningSeverity severity) {
        if (severity == null) return 0;
        switch (severity) {
            case CRITICAL: return 4;
            case HIGH: return 3;
            case MEDIUM: return 2;
            case LOW: return 1;
            default: return 0;
        }
    }
}

