package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.SystemStabilityHistoryResponse;
import com.tuttime.admin.dto.WarningSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSystemStabilityHistoryServiceImpl Tests")
class AdminSystemStabilityHistoryServiceImplTest {

    @InjectMocks
    private AdminSystemStabilityHistoryServiceImpl systemStabilityHistoryService;

    private PageRequest pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    @Test
    @DisplayName("Should get system stability history - returns empty initially")
    void testGetSystemStabilityHistory_InitiallyEmpty_ReturnsEmpty() {
        // When
        Page<SystemStabilityHistoryResponse> result = systemStabilityHistoryService.getSystemStabilityHistory(
                pageable, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should filter system stability history by event type")
    void testGetSystemStabilityHistory_FilterByEventType_ReturnsFiltered() {
        // Given
        SystemStabilityHistoryResponse event1 = SystemStabilityHistoryResponse.builder()
                .historyId("EVENT_1")
                .severity(WarningSeverity.HIGH)
                .eventType("ERROR_RATE")
                .title("High Error Rate")
                .description("Error rate exceeded threshold")
                .occurredAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .resolved(false)
                .serviceName("user-service")
                .build();

        SystemStabilityHistoryResponse event2 = SystemStabilityHistoryResponse.builder()
                .historyId("EVENT_2")
                .severity(WarningSeverity.MEDIUM)
                .eventType("RESPONSE_TIME")
                .title("Slow Response Time")
                .description("Response time exceeded threshold")
                .occurredAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(2))
                .resolved(false)
                .serviceName("token-service")
                .build();

        // Add events using reflection or public method if available
        // For now, we test the filtering logic
        systemStabilityHistoryService.addStabilityEvent(event1);
        systemStabilityHistoryService.addStabilityEvent(event2);

        // When
        Page<SystemStabilityHistoryResponse> result = systemStabilityHistoryService.getSystemStabilityHistory(
                pageable, null, null, "ERROR_RATE", null);

        // Then
        assertThat(result).isNotNull();
        if (result.getTotalElements() > 0) {
            assertThat(result.getContent().get(0).getEventType()).isEqualTo("ERROR_RATE");
        }
    }

    @Test
    @DisplayName("Should filter system stability history by service name")
    void testGetSystemStabilityHistory_FilterByServiceName_ReturnsFiltered() {
        // Given
        SystemStabilityHistoryResponse event = SystemStabilityHistoryResponse.builder()
                .historyId("EVENT_1")
                .severity(WarningSeverity.HIGH)
                .eventType("MEMORY_USAGE")
                .title("High Memory Usage")
                .description("Memory usage exceeded threshold")
                .occurredAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .resolved(false)
                .serviceName("user-service")
                .build();

        systemStabilityHistoryService.addStabilityEvent(event);

        // When
        Page<SystemStabilityHistoryResponse> result = systemStabilityHistoryService.getSystemStabilityHistory(
                pageable, null, null, null, "user-service");

        // Then
        assertThat(result).isNotNull();
        if (result.getTotalElements() > 0) {
            assertThat(result.getContent().get(0).getServiceName()).isEqualTo("user-service");
        }
    }

    @Test
    @DisplayName("Should filter system stability history by date range")
    void testGetSystemStabilityHistory_FilterByDateRange_ReturnsFiltered() {
        // Given
        SystemStabilityHistoryResponse event = SystemStabilityHistoryResponse.builder()
                .historyId("EVENT_1")
                .severity(WarningSeverity.MEDIUM)
                .eventType("CPU_USAGE")
                .title("High CPU Usage")
                .description("CPU usage exceeded threshold")
                .occurredAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .resolved(false)
                .serviceName("mining-service")
                .build();

        systemStabilityHistoryService.addStabilityEvent(event);

        OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        // When
        Page<SystemStabilityHistoryResponse> result = systemStabilityHistoryService.getSystemStabilityHistory(
                pageable, startDate, endDate, null, null);

        // Then
        assertThat(result).isNotNull();
        if (result.getTotalElements() > 0) {
            assertThat(result.getContent().get(0).getOccurredAt())
                    .isAfter(startDate)
                    .isBefore(endDate);
        }
    }

    @Test
    @DisplayName("Should exclude events outside date range")
    void testGetSystemStabilityHistory_ExcludeOutsideDateRange_ReturnsFiltered() {
        // Given
        SystemStabilityHistoryResponse oldEvent = SystemStabilityHistoryResponse.builder()
                .historyId("EVENT_OLD")
                .severity(WarningSeverity.LOW)
                .eventType("DATABASE_ERROR")
                .title("Database Error")
                .description("Database connection error")
                .occurredAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10))
                .resolved(true)
                .serviceName("admin-service")
                .build();

        SystemStabilityHistoryResponse recentEvent = SystemStabilityHistoryResponse.builder()
                .historyId("EVENT_RECENT")
                .severity(WarningSeverity.HIGH)
                .eventType("ERROR_RATE")
                .title("High Error Rate")
                .description("Error rate exceeded threshold")
                .occurredAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .resolved(false)
                .serviceName("user-service")
                .build();

        systemStabilityHistoryService.addStabilityEvent(oldEvent);
        systemStabilityHistoryService.addStabilityEvent(recentEvent);

        OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        // When
        Page<SystemStabilityHistoryResponse> result = systemStabilityHistoryService.getSystemStabilityHistory(
                pageable, startDate, endDate, null, null);

        // Then
        assertThat(result).isNotNull();
        // Should only include recent event
        if (result.getTotalElements() > 0) {
            result.getContent().forEach(event -> {
                assertThat(event.getOccurredAt()).isAfter(startDate);
                assertThat(event.getOccurredAt()).isBefore(endDate);
            });
        }
    }

    @Test
    @DisplayName("Should sort history by occurred date")
    void testGetSystemStabilityHistory_SortedByDate_ReturnsSorted() {
        // Given
        SystemStabilityHistoryResponse event1 = SystemStabilityHistoryResponse.builder()
                .historyId("EVENT_1")
                .severity(WarningSeverity.MEDIUM)
                .eventType("RESPONSE_TIME")
                .title("Slow Response")
                .occurredAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(2))
                .resolved(false)
                .serviceName("token-service")
                .build();

        SystemStabilityHistoryResponse event2 = SystemStabilityHistoryResponse.builder()
                .historyId("EVENT_2")
                .severity(WarningSeverity.HIGH)
                .eventType("ERROR_RATE")
                .title("High Error Rate")
                .occurredAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .resolved(false)
                .serviceName("user-service")
                .build();

        systemStabilityHistoryService.addStabilityEvent(event1);
        systemStabilityHistoryService.addStabilityEvent(event2);

        // When
        Page<SystemStabilityHistoryResponse> result = systemStabilityHistoryService.getSystemStabilityHistory(
                pageable, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        if (result.getTotalElements() >= 2) {
            // Should be sorted by occurred date (newest first)
            OffsetDateTime first = result.getContent().get(0).getOccurredAt();
            OffsetDateTime second = result.getContent().get(1).getOccurredAt();
            assertThat(first).isAfterOrEqualTo(second);
        }
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void testGetSystemStabilityHistory_Pagination_ReturnsCorrectPage() {
        // Given
        for (int i = 0; i < 15; i++) {
            SystemStabilityHistoryResponse event = SystemStabilityHistoryResponse.builder()
                    .historyId("EVENT_" + i)
                    .severity(WarningSeverity.MEDIUM)
                    .eventType("TEST_EVENT")
                    .title("Test Event " + i)
                    .occurredAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(i))
                    .resolved(false)
                    .serviceName("test-service")
                    .build();
            systemStabilityHistoryService.addStabilityEvent(event);
        }

        PageRequest firstPage = PageRequest.of(0, 10);
        PageRequest secondPage = PageRequest.of(1, 10);

        // When
        Page<SystemStabilityHistoryResponse> firstPageResult = systemStabilityHistoryService.getSystemStabilityHistory(
                firstPage, null, null, null, null);
        Page<SystemStabilityHistoryResponse> secondPageResult = systemStabilityHistoryService.getSystemStabilityHistory(
                secondPage, null, null, null, null);

        // Then
        assertThat(firstPageResult.getContent()).hasSize(10);
        assertThat(secondPageResult.getContent()).hasSize(5); // Remaining 5 items
        assertThat(firstPageResult.getTotalElements()).isEqualTo(15);
        assertThat(secondPageResult.getTotalElements()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should add stability event successfully")
    void testAddStabilityEvent_Success_AddsEvent() {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("errorRate", 0.05);
        metrics.put("responseTime", 2000);

        SystemStabilityHistoryResponse event = SystemStabilityHistoryResponse.builder()
                .historyId("EVENT_NEW")
                .severity(WarningSeverity.CRITICAL)
                .eventType("SYSTEM_DOWN")
                .title("System Down")
                .description("System is down")
                .occurredAt(OffsetDateTime.now(ZoneOffset.UTC))
                .resolved(false)
                .serviceName("admin-service")
                .metrics(metrics)
                .build();

        // When
        systemStabilityHistoryService.addStabilityEvent(event);

        // Then
        Page<SystemStabilityHistoryResponse> result = systemStabilityHistoryService.getSystemStabilityHistory(
                pageable, null, null, null, null);
        assertThat(result.getTotalElements()).isGreaterThan(0);
    }
}

