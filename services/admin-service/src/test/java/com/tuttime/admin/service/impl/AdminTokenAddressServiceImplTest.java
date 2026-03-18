package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.BlockedAddressResponse;
import com.tuttime.admin.dto.WhitelistedAddressResponse;
import com.tuttime.admin.service.AdminTokenAddressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTokenAddressServiceImpl Tests")
class AdminTokenAddressServiceImplTest {

    @InjectMocks
    private AdminTokenAddressServiceImpl addressService;

    @Test
    @DisplayName("Should list blocked addresses - returns empty page (placeholder)")
    void testListBlockedAddresses_Placeholder_ReturnsEmptyPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);

        // When
        Page<BlockedAddressResponse> result = addressService.listBlockedAddresses(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should list whitelisted addresses - returns empty page (placeholder)")
    void testListWhitelistedAddresses_Placeholder_ReturnsEmptyPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);

        // When
        Page<WhitelistedAddressResponse> result = addressService.listWhitelistedAddresses(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }
}

