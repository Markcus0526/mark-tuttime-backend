package com.tuttime.payment.controller;

import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies the test-only endpoint is gated: disabled by default (cluster/prod), enabled only when
 * app.test-endpoints.enabled=true (local/dev).
 */
class PaymentControllerTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentController controller = new PaymentController(paymentService);

    @Test
    void testEndpointRejectedWhenDisabled() {
        ReflectionTestUtils.setField(controller, "testEndpointsEnabled", false);
        assertThrows(ForbiddenException.class, () -> controller.testConfirmPayment(UUID.randomUUID()));
        verifyNoInteractions(paymentService);
    }

    @Test
    void testEndpointAllowedWhenEnabled() {
        ReflectionTestUtils.setField(controller, "testEndpointsEnabled", true);
        controller.testConfirmPayment(UUID.randomUUID());
        verify(paymentService).testManualPaymentSuccess(any(), anyString());
    }
}
