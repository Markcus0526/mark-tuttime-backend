package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.BlockedDeviceResponse;
import com.tuttime.admin.dto.BlockedIpResponse;
import com.tuttime.admin.dto.WhitelistedDeviceResponse;
import com.tuttime.admin.dto.WhitelistedIpResponse;
import com.tuttime.admin.service.AdminAntiFraudService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAntiFraudServiceImpl Tests")
class AdminAntiFraudServiceImplTest {

    @InjectMocks
    private AdminAntiFraudServiceImpl antiFraudService;

    @Test
    @DisplayName("Should list blocked devices - returns empty page (placeholder)")
    void testListBlockedDevices_Placeholder_ReturnsEmptyPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);

        // When
        Page<BlockedDeviceResponse> result = antiFraudService.listBlockedDevices(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should list blocked IPs - returns empty page (placeholder)")
    void testListBlockedIps_Placeholder_ReturnsEmptyPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);

        // When
        Page<BlockedIpResponse> result = antiFraudService.listBlockedIps(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should list whitelisted devices - returns empty page (placeholder)")
    void testListWhitelistedDevices_Placeholder_ReturnsEmptyPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);

        // When
        Page<WhitelistedDeviceResponse> result = antiFraudService.listWhitelistedDevices(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should list whitelisted IPs - returns empty page (placeholder)")
    void testListWhitelistedIps_Placeholder_ReturnsEmptyPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);

        // When
        Page<WhitelistedIpResponse> result = antiFraudService.listWhitelistedIps(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }
}

