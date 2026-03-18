package com.tuttime.admin.service;

import com.tuttime.admin.dto.AuditLogResponse;
import com.tuttime.admin.entity.AdminAuditLog;
import com.tuttime.admin.repository.AdminAuditLogRepository;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuditService Tests")
class AdminAuditServiceTest {

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminAuditService auditService;

    private PageRequest pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    @Test
    @DisplayName("Should get audit logs without filters")
    void testGetAuditLogs_NoFilters_ReturnsAllLogs() {
        // Given
        AdminAuditLog log1 = AdminAuditLog.builder()
                .logId(1L)
                .adminId(100L)
                .adminUsername("admin1")
                .actionType(AdminAuditLog.AdminActionType.USER_VIEWED)
                .resourceType("USER")
                .resourceId(200L)
                .description("Viewed user")
                .ipAddress("192.168.1.1")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .build();

        AdminAuditLog log2 = AdminAuditLog.builder()
                .logId(2L)
                .adminId(100L)
                .adminUsername("admin1")
                .actionType(AdminAuditLog.AdminActionType.USER_UPDATED)
                .resourceType("USER")
                .resourceId(200L)
                .description("Updated user")
                .ipAddress("192.168.1.1")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        Page<AdminAuditLog> logsPage = new PageImpl<>(Arrays.asList(log1, log2), pageable, 2);
        when(auditLogRepository.findAll(pageable)).thenReturn(logsPage);

        // When
        Page<AuditLogResponse> result = auditService.getAuditLogs(
                null, null, null, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        verify(auditLogRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should get audit logs filtered by admin ID")
    void testGetAuditLogs_FilterByAdminId_ReturnsFiltered() {
        // Given
        Long adminId = 100L;
        AdminAuditLog log = AdminAuditLog.builder()
                .logId(1L)
                .adminId(adminId)
                .adminUsername("admin1")
                .actionType(AdminAuditLog.AdminActionType.USER_VIEWED)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        Page<AdminAuditLog> logsPage = new PageImpl<>(Collections.singletonList(log), pageable, 1);
        when(auditLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId, pageable)).thenReturn(logsPage);

        // When
        Page<AuditLogResponse> result = auditService.getAuditLogs(
                adminId, null, null, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAdminId()).isEqualTo(adminId);
        verify(auditLogRepository).findByAdminIdOrderByCreatedAtDesc(adminId, pageable);
    }

    @Test
    @DisplayName("Should get audit logs filtered by action type")
    void testGetAuditLogs_FilterByActionType_ReturnsFiltered() {
        // Given
        AdminAuditLog.AdminActionType actionType = AdminAuditLog.AdminActionType.USER_BLOCKED;
        AdminAuditLog log = AdminAuditLog.builder()
                .logId(1L)
                .adminId(100L)
                .actionType(actionType)
                .resourceType("USER")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        Page<AdminAuditLog> logsPage = new PageImpl<>(Collections.singletonList(log), pageable, 1);
        when(auditLogRepository.findByActionTypeOrderByCreatedAtDesc(actionType, pageable)).thenReturn(logsPage);

        // When
        Page<AuditLogResponse> result = auditService.getAuditLogs(
                null, actionType, null, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getActionType()).isEqualTo(actionType);
        verify(auditLogRepository).findByActionTypeOrderByCreatedAtDesc(actionType, pageable);
    }

    @Test
    @DisplayName("Should get audit logs filtered by date range")
    void testGetAuditLogs_FilterByDateRange_ReturnsFiltered() {
        // Given
        OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC);
        
        AdminAuditLog log = AdminAuditLog.builder()
                .logId(1L)
                .adminId(100L)
                .actionType(AdminAuditLog.AdminActionType.USER_VIEWED)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(12))
                .build();

        Page<AdminAuditLog> logsPage = new PageImpl<>(Collections.singletonList(log), pageable, 1);
        when(auditLogRepository.findByDateRange(startDate, endDate, pageable)).thenReturn(logsPage);

        // When
        Page<AuditLogResponse> result = auditService.getAuditLogs(
                null, null, null, startDate, endDate, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(auditLogRepository).findByDateRange(startDate, endDate, pageable);
    }

    @Test
    @DisplayName("Should get audit logs filtered by admin and date range")
    void testGetAuditLogs_FilterByAdminAndDateRange_ReturnsFiltered() {
        // Given
        Long adminId = 100L;
        OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC);
        
        AdminAuditLog log = AdminAuditLog.builder()
                .logId(1L)
                .adminId(adminId)
                .actionType(AdminAuditLog.AdminActionType.USER_UPDATED)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(12))
                .build();

        Page<AdminAuditLog> logsPage = new PageImpl<>(Collections.singletonList(log), pageable, 1);
        when(auditLogRepository.findByAdminAndDateRange(adminId, startDate, endDate, pageable)).thenReturn(logsPage);

        // When
        Page<AuditLogResponse> result = auditService.getAuditLogs(
                adminId, null, null, startDate, endDate, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAdminId()).isEqualTo(adminId);
        verify(auditLogRepository).findByAdminAndDateRange(adminId, startDate, endDate, pageable);
    }

    @Test
    @DisplayName("Should filter audit logs by resource type")
    void testGetAuditLogs_FilterByResourceType_ReturnsFiltered() {
        // Given
        String resourceType = "TOKEN";
        AdminAuditLog log1 = AdminAuditLog.builder()
                .logId(1L)
                .adminId(100L)
                .actionType(AdminAuditLog.AdminActionType.TOKEN_BALANCE_ADJUSTED)
                .resourceType(resourceType)
                .resourceId(500L)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        AdminAuditLog log2 = AdminAuditLog.builder()
                .logId(2L)
                .adminId(100L)
                .actionType(AdminAuditLog.AdminActionType.USER_VIEWED)
                .resourceType("USER")
                .resourceId(200L)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        Page<AdminAuditLog> logsPage = new PageImpl<>(Arrays.asList(log1, log2), pageable, 2);
        when(auditLogRepository.findAll(pageable)).thenReturn(logsPage);

        // When
        Page<AuditLogResponse> result = auditService.getAuditLogs(
                null, null, resourceType, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getResourceType()).isEqualTo(resourceType);
    }

    @Test
    @DisplayName("Should convert audit log entity to response DTO correctly")
    void testGetAuditLogs_ConvertsEntityToResponse_Correctly() {
        // Given
        AdminAuditLog log = AdminAuditLog.builder()
                .logId(1L)
                .adminId(100L)
                .adminUsername("admin1")
                .actionType(AdminAuditLog.AdminActionType.USER_VIEWED)
                .resourceType("USER")
                .resourceId(200L)
                .description("Viewed user details")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestMethod("GET")
                .requestPath("/api/admin/users/200")
                .requestBody(null)
                .responseStatus(200)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        Page<AdminAuditLog> logsPage = new PageImpl<>(Collections.singletonList(log), pageable, 1);
        when(auditLogRepository.findAll(pageable)).thenReturn(logsPage);

        // When
        Page<AuditLogResponse> result = auditService.getAuditLogs(
                null, null, null, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        
        AuditLogResponse response = result.getContent().get(0);
        assertThat(response.getLogId()).isEqualTo(1L);
        assertThat(response.getAdminId()).isEqualTo(100L);
        assertThat(response.getAdminUsername()).isEqualTo("admin1");
        assertThat(response.getActionType()).isEqualTo(AdminAuditLog.AdminActionType.USER_VIEWED);
        assertThat(response.getResourceType()).isEqualTo("USER");
        assertThat(response.getResourceId()).isEqualTo(200L);
        assertThat(response.getDescription()).isEqualTo("Viewed user details");
        assertThat(response.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(response.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(response.getRequestMethod()).isEqualTo("GET");
        assertThat(response.getRequestPath()).isEqualTo("/api/admin/users/200");
        assertThat(response.getResponseStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should handle empty audit logs")
    void testGetAuditLogs_EmptyLogs_ReturnsEmptyPage() {
        // Given
        Page<AdminAuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(auditLogRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<AuditLogResponse> result = auditService.getAuditLogs(
                null, null, null, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}

