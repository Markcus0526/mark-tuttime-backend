package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.AdRequest;
import com.tuttime.admin.dto.AdResponse;
import com.tuttime.admin.rest.client.ContentServiceRestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAdServiceImpl Tests")
class AdminAdServiceImplTest {

    @Mock
    private ContentServiceRestClient contentServiceRestClient;

    @InjectMocks
    private AdminAdServiceImpl adminAdService;

    @Test
    @DisplayName("Should list ads successfully")
    void testListAds_Success_ReturnsPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        AdResponse response = AdResponse.builder()
                .adId(1L)
                .adTitle("Test Ad")
                .status("ACTIVE")
                .build();

        Page<AdResponse> page = new PageImpl<>(Arrays.asList(response), pageable, 1);
        when(contentServiceRestClient.listAds(pageable, "ACTIVE")).thenReturn(page);

        // When
        Page<AdResponse> result = adminAdService.listAds(pageable, "ACTIVE");

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should get ad by ID successfully")
    void testGetAd_Success_ReturnsAd() {
        // Given
        Long id = 1L;
        AdResponse response = AdResponse.builder()
                .adId(id)
                .adTitle("Test Ad")
                .build();

        when(contentServiceRestClient.getAd(id)).thenReturn(response);

        // When
        AdResponse result = adminAdService.getAd(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAdId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should create ad successfully")
    void testCreateAd_Success() {
        // Given
        AdRequest request = AdRequest.builder()
                .adTitle("New Ad")
                .adType("BANNER")
                .paymentAmount(new BigDecimal("1000"))
                .build();

        AdResponse response = AdResponse.builder()
                .adId(1L)
                .adTitle("New Ad")
                .build();

        when(contentServiceRestClient.createAd(request)).thenReturn(response);

        // When
        AdResponse result = adminAdService.createAd(request);

        // Then
        assertThat(result).isNotNull();
        verify(contentServiceRestClient).createAd(request);
    }

    @Test
    @DisplayName("Should update ad successfully")
    void testUpdateAd_Success() {
        // Given
        Long id = 1L;
        AdRequest request = AdRequest.builder()
                .adTitle("Updated Title")
                .build();

        AdResponse response = AdResponse.builder()
                .adId(id)
                .adTitle("Updated Title")
                .build();

        when(contentServiceRestClient.updateAd(id, request)).thenReturn(response);

        // When
        AdResponse result = adminAdService.updateAd(id, request);

        // Then
        assertThat(result).isNotNull();
        verify(contentServiceRestClient).updateAd(id, request);
    }

    @Test
    @DisplayName("Should delete ad successfully")
    void testDeleteAd_Success() {
        // Given
        Long id = 1L;
        doNothing().when(contentServiceRestClient).deleteAd(id);

        // When
        adminAdService.deleteAd(id);

        // Then
        verify(contentServiceRestClient).deleteAd(id);
    }

    @Test
    @DisplayName("Should approve ad successfully")
    void testApproveAd_Success() {
        // Given
        Long id = 1L;
        Long adminId = 100L;
        AdResponse response = AdResponse.builder()
                .adId(id)
                .status("APPROVED")
                .build();

        when(contentServiceRestClient.approveAd(id, adminId)).thenReturn(response);

        // When
        AdResponse result = adminAdService.approveAd(id, adminId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Should reject ad successfully")
    void testRejectAd_Success() {
        // Given
        Long id = 1L;
        Long adminId = 100L;
        AdResponse response = AdResponse.builder()
                .adId(id)
                .status("REJECTED")
                .build();

        com.tuttime.admin.dto.RejectRequest rejectRequest = com.tuttime.admin.dto.RejectRequest.builder()
                .reason("Test rejection reason")
                .build();

        when(contentServiceRestClient.rejectAd(id, adminId, rejectRequest)).thenReturn(response);

        // When
        AdResponse result = adminAdService.rejectAd(id, adminId, rejectRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("REJECTED");
    }
}

