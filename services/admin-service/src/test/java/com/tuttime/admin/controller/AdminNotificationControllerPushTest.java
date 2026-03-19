package com.tuttime.admin.controller;

import com.tuttime.admin.rest.client.MiningServiceRestClient;
import com.tuttime.admin.rest.client.NotificationServiceRestClient;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminUserManagementService;
import com.tuttime.admin.kafka.CampaignEventProducer;
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
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for push notification workflows to inactive and never-started mining users.
 * 
 * Tests verify:
 * 1. Correct filtering of inactive users (miningActive = false)
 * 2. Correct filtering of never-started users (miningActive = null)
 * 3. Proper handling of edge cases (null values, missing records, etc.)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminNotificationController Push Notification Tests")
class AdminNotificationControllerPushTest {

    @Mock
    private MiningServiceRestClient miningServiceRestClient;

    @Mock
    private NotificationServiceRestClient notificationServiceRestClient;

    @Mock
    private AdminAuditService auditService;

    @Mock
    private AdminUserManagementService adminUserManagementService;

    @Mock
    private CampaignEventProducer campaignEventProducer;

    @InjectMocks
    private AdminNotificationController controller;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 1000);
    }

    @Test
    @DisplayName("Inactive users: Should filter for miningActive = false only")
    void testInactiveUsers_FiltersCorrectly() throws Exception {
        // Arrange: Create test data with different miningActive values
        List<Map<String, Object>> balances = Arrays.asList(
            createBalance(1L, false),  // Inactive - should be included
            createBalance(2L, true),   // Active - should be excluded
            createBalance(3L, null),   // Never started - should be excluded
            createBalance(4L, false),  // Inactive - should be included
            createBalance(5L, true)    // Active - should be excluded
        );

        Page<Map<String, Object>> balancesPage = new PageImpl<>(balances, pageable, balances.size());
        
        when(miningServiceRestClient.listApBalances(any(Pageable.class)))
            .thenReturn(balancesPage);
        
        Map<String, Object> successResponse = createSuccessNotificationResponse(2);
        when(notificationServiceRestClient.sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString()))
            .thenReturn(successResponse);

        // Act: Call the private method via reflection
        java.lang.reflect.Method method = AdminNotificationController.class.getDeclaredMethod(
            "processPushNotifications",
            Long.class, String.class, String.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, 1L, "Test Title", "Test Body", null, "NORMAL");
        
        // Assert: Verify notifications were sent only to inactive users (1L and 4L)
        verify(notificationServiceRestClient, times(2)).sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString());
        verify(notificationServiceRestClient).sendPushNotification(
            eq(1L), anyString(), anyString(), any(), anyString());
        verify(notificationServiceRestClient).sendPushNotification(
            eq(4L), anyString(), anyString(), any(), anyString());
        // Verify active users (2L, 5L) were NOT called
        verify(notificationServiceRestClient, never()).sendPushNotification(
            eq(2L), anyString(), anyString(), any(), anyString());
        verify(notificationServiceRestClient, never()).sendPushNotification(
            eq(5L), anyString(), anyString(), any(), anyString());
        // Verify never-started user (3L) was NOT called
        verify(notificationServiceRestClient, never()).sendPushNotification(
            eq(3L), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Never-started users: Should filter for miningActive = null only")
    void testNeverStartedUsers_FiltersCorrectly() throws Exception {
        // Arrange: Create test data
        List<Map<String, Object>> balances = Arrays.asList(
            createBalance(1L, null),   // Never started - should be included
            createBalance(2L, false), // Inactive - should be EXCLUDED (fixed in code)
            createBalance(3L, true),  // Active - should be excluded
            createBalance(4L, null),  // Never started - should be included
            createBalance(5L, false)  // Inactive - should be EXCLUDED (fixed in code)
        );

        Page<Map<String, Object>> balancesPage = new PageImpl<>(balances, pageable, balances.size());
        
        when(miningServiceRestClient.listApBalances(any(Pageable.class)))
            .thenReturn(balancesPage);
        
        Map<String, Object> successResponse = createSuccessNotificationResponse(1);
        when(notificationServiceRestClient.sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString()))
            .thenReturn(successResponse);

        // Act: Call the private method via reflection
        java.lang.reflect.Method method = AdminNotificationController.class.getDeclaredMethod(
            "processPushNotificationsToNeverStartedUsers",
            Long.class, String.class, String.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, 1L, "Test Title", "Test Body", null, "NORMAL");
        
        // Assert: Verify notifications were sent only to never-started users (1L and 4L)
        // After fix: Should only include null values, exclude false values
        verify(notificationServiceRestClient, times(2)).sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString());
        verify(notificationServiceRestClient).sendPushNotification(
            eq(1L), anyString(), anyString(), any(), anyString());
        verify(notificationServiceRestClient).sendPushNotification(
            eq(4L), anyString(), anyString(), any(), anyString());
        // Verify inactive users (2L, 5L) were NOT called (fixed behavior)
        verify(notificationServiceRestClient, never()).sendPushNotification(
            eq(2L), anyString(), anyString(), any(), anyString());
        verify(notificationServiceRestClient, never()).sendPushNotification(
            eq(5L), anyString(), anyString(), any(), anyString());
        // Verify active user (3L) was NOT called
        verify(notificationServiceRestClient, never()).sendPushNotification(
            eq(3L), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Inactive users: Should handle null miningActive correctly")
    void testInactiveUsers_HandlesNullMiningActive() throws Exception {
        // Arrange: Balance with null miningActive (never started)
        List<Map<String, Object>> balances = Arrays.asList(
            createBalance(1L, null)  // Never started - should be skipped
        );

        Page<Map<String, Object>> balancesPage = new PageImpl<>(balances, pageable, balances.size());
        
        when(miningServiceRestClient.listApBalances(any(Pageable.class)))
            .thenReturn(balancesPage);

        // Act: Process inactive users via reflection
        java.lang.reflect.Method method = AdminNotificationController.class.getDeclaredMethod(
            "processPushNotifications",
            Long.class, String.class, String.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, 1L, "Test Title", "Test Body", null, "NORMAL");
        
        // Assert: No notifications should be sent (null miningActive means never started, not inactive)
        verify(notificationServiceRestClient, never()).sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Never-started users: Should handle false miningActive correctly")
    void testNeverStartedUsers_HandlesFalseMiningActive() throws Exception {
        // Arrange: Balance with false miningActive (inactive, not never-started)
        List<Map<String, Object>> balances = Arrays.asList(
            createBalance(1L, false)  // Inactive - should be EXCLUDED from never-started
        );

        Page<Map<String, Object>> balancesPage = new PageImpl<>(balances, pageable, balances.size());
        
        when(miningServiceRestClient.listApBalances(any(Pageable.class)))
            .thenReturn(balancesPage);

        // Act: Process never-started users via reflection
        java.lang.reflect.Method method = AdminNotificationController.class.getDeclaredMethod(
            "processPushNotificationsToNeverStartedUsers",
            Long.class, String.class, String.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, 1L, "Test Title", "Test Body", null, "NORMAL");
        
        // Assert: After fix, notification should NOT be sent (false means inactive, not never-started)
        verify(notificationServiceRestClient, never()).sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Inactive users: Should handle string miningActive values")
    void testInactiveUsers_HandlesStringMiningActive() throws Exception {
        // Arrange: Balance with string "false" miningActive
        Map<String, Object> balance = new HashMap<>();
        balance.put("userId", 1L);
        balance.put("miningActive", "false");  // String representation
        balance.put("currentBalance", "0");
        balance.put("monthlyAccumulated", "0");
        balance.put("lifetimeAccumulated", "0");

        List<Map<String, Object>> balances = Arrays.asList(balance);
        Page<Map<String, Object>> balancesPage = new PageImpl<>(balances, pageable, balances.size());
        
        when(miningServiceRestClient.listApBalances(any(Pageable.class)))
            .thenReturn(balancesPage);
        
        Map<String, Object> successResponse = createSuccessNotificationResponse(1);
        when(notificationServiceRestClient.sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString()))
            .thenReturn(successResponse);

        // Act: Call the private method via reflection
        java.lang.reflect.Method method = AdminNotificationController.class.getDeclaredMethod(
            "processPushNotifications",
            Long.class, String.class, String.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, 1L, "Test Title", "Test Body", null, "NORMAL");
        
        // Assert: Should parse string "false" as Boolean false and send notification
        verify(notificationServiceRestClient, times(1)).sendPushNotification(
            eq(1L), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Inactive users: Should handle empty page")
    void testInactiveUsers_HandlesEmptyPage() throws Exception {
        // Arrange: Empty page
        Page<Map<String, Object>> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        when(miningServiceRestClient.listApBalances(any(Pageable.class)))
            .thenReturn(emptyPage);

        // Act: Process inactive users via reflection
        java.lang.reflect.Method method = AdminNotificationController.class.getDeclaredMethod(
            "processPushNotifications",
            Long.class, String.class, String.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, 1L, "Test Title", "Test Body", null, "NORMAL");
        
        // Assert: No notifications should be sent
        verify(notificationServiceRestClient, never()).sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Inactive users: Should handle pagination correctly")
    void testInactiveUsers_HandlesPagination() throws Exception {
        // Arrange: Multiple pages - simulate more than 1000 users to trigger pagination
        // Create 1000 inactive users for page 1, and 2 more for page 2
        List<Map<String, Object>> page1 = new ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            page1.add(createBalance(i, false));
        }
        
        List<Map<String, Object>> page2 = Arrays.asList(
            createBalance(1001L, false),
            createBalance(1002L, true)  // Active - should be excluded
        );

        // Total is 1002, so page 0 (size 1000) is NOT last, page 1 (size 1000) IS last
        Page<Map<String, Object>> page1Response = new PageImpl<>(page1, PageRequest.of(0, 1000), 1002);
        Page<Map<String, Object>> page2Response = new PageImpl<>(page2, PageRequest.of(1, 1000), 1002);
        
        when(miningServiceRestClient.listApBalances(PageRequest.of(0, 1000)))
            .thenReturn(page1Response);
        when(miningServiceRestClient.listApBalances(PageRequest.of(1, 1000)))
            .thenReturn(page2Response);
        
        Map<String, Object> successResponse = createSuccessNotificationResponse(1);
        when(notificationServiceRestClient.sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString()))
            .thenReturn(successResponse);

        // Act: Process inactive users via reflection
        java.lang.reflect.Method method = AdminNotificationController.class.getDeclaredMethod(
            "processPushNotifications",
            Long.class, String.class, String.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, 1L, "Test Title", "Test Body", null, "NORMAL");
        
        // Assert: Should process both pages and send notifications to inactive users
        verify(miningServiceRestClient, atLeastOnce()).listApBalances(PageRequest.of(0, 1000));
        verify(miningServiceRestClient, atLeastOnce()).listApBalances(PageRequest.of(1, 1000));
        // Should send notifications to 1001 inactive users (1000 from page1 + 1 from page2)
        verify(notificationServiceRestClient, times(1001)).sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString());
        // Verify active user (1002L) was NOT called
        verify(notificationServiceRestClient, never()).sendPushNotification(
            eq(1002L), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Inactive users: Should handle notification failures gracefully")
    void testInactiveUsers_HandlesNotificationFailures() throws Exception {
        // Arrange: Balance with inactive user
        List<Map<String, Object>> balances = Arrays.asList(
            createBalance(1L, false)
        );

        Page<Map<String, Object>> balancesPage = new PageImpl<>(balances, pageable, balances.size());
        
        when(miningServiceRestClient.listApBalances(any(Pageable.class)))
            .thenReturn(balancesPage);
        
        // Notification fails
        when(notificationServiceRestClient.sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString()))
            .thenThrow(new RuntimeException("Notification service unavailable"));

        // Act: Process inactive users via reflection
        java.lang.reflect.Method method = AdminNotificationController.class.getDeclaredMethod(
            "processPushNotifications",
            Long.class, String.class, String.class, Map.class, String.class);
        method.setAccessible(true);
        // Should not throw exception - should handle gracefully
        assertDoesNotThrow(() -> {
            method.invoke(controller, 1L, "Test Title", "Test Body", null, "NORMAL");
        });
        
        // Assert: Should handle exception gracefully and continue processing
        // Current implementation catches exceptions and logs them
        verify(notificationServiceRestClient, times(1)).sendPushNotification(
            eq(1L), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Never-started users: Should only include null miningActive")
    void testNeverStartedUsers_OnlyIncludesNull() throws Exception {
        // Arrange: Mix of null, false, and true
        List<Map<String, Object>> balances = Arrays.asList(
            createBalance(1L, null),   // Should be included
            createBalance(2L, null),   // Should be included
            createBalance(3L, false),  // Should be EXCLUDED (inactive, not never-started)
            createBalance(4L, true),   // Should be excluded
            createBalance(5L, null)    // Should be included
        );

        Page<Map<String, Object>> balancesPage = new PageImpl<>(balances, pageable, balances.size());
        
        when(miningServiceRestClient.listApBalances(any(Pageable.class)))
            .thenReturn(balancesPage);
        
        Map<String, Object> successResponse = createSuccessNotificationResponse(1);
        when(notificationServiceRestClient.sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString()))
            .thenReturn(successResponse);

        // Act: Call the private method via reflection
        java.lang.reflect.Method method = AdminNotificationController.class.getDeclaredMethod(
            "processPushNotificationsToNeverStartedUsers",
            Long.class, String.class, String.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, 1L, "Test Title", "Test Body", null, "NORMAL");
        
        // Assert: Should only include users 1, 2, 5 (null only), exclude 3 (false) and 4 (true)
        verify(notificationServiceRestClient, times(3)).sendPushNotification(
            anyLong(), anyString(), anyString(), any(), anyString());
        verify(notificationServiceRestClient).sendPushNotification(
            eq(1L), anyString(), anyString(), any(), anyString());
        verify(notificationServiceRestClient).sendPushNotification(
            eq(2L), anyString(), anyString(), any(), anyString());
        verify(notificationServiceRestClient).sendPushNotification(
            eq(5L), anyString(), anyString(), any(), anyString());
        // Verify inactive user (3L) was NOT called
        verify(notificationServiceRestClient, never()).sendPushNotification(
            eq(3L), anyString(), anyString(), any(), anyString());
        // Verify active user (4L) was NOT called
        verify(notificationServiceRestClient, never()).sendPushNotification(
            eq(4L), anyString(), anyString(), any(), anyString());
    }

    // Helper methods

    private Map<String, Object> createBalance(Long userId, Boolean miningActive) {
        Map<String, Object> balance = new HashMap<>();
        balance.put("userId", userId);
        balance.put("miningActive", miningActive);
        balance.put("currentBalance", "0");
        balance.put("monthlyAccumulated", "0");
        balance.put("lifetimeAccumulated", "0");
        return balance;
    }

    private Map<String, Object> createSuccessNotificationResponse(int devicesSent) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("devicesSent", devicesSent);
        response.put("message", "Notification sent successfully");
        return response;
    }
}

