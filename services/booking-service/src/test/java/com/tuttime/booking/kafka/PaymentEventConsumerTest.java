package com.tuttime.booking.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.event.kafka.event.PaymentEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies the silent-message-loss fix: the consumer must RETHROW on processing failure so the
 * Kafka error handler / DLQ engages, rather than swallowing the exception.
 */
class PaymentEventConsumerTest {

    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final BookingService bookingService = mock(BookingService.class);
    private final PaymentEventConsumer consumer = new PaymentEventConsumer(objectMapper, bookingService);

    @Test
    void rethrowsWhenProcessingFails() throws Exception {
        PaymentEvent.PaymentCompletedEvent event = new PaymentEvent.PaymentCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "ext-pay-1", null, null, "RUB", null, null);
        when(objectMapper.readValue(anyString(), eq(PaymentEvent.PaymentCompletedEvent.class))).thenReturn(event);
        doThrow(new RuntimeException("downstream down"))
                .when(bookingService).markAsPaid(any(), any(), any(), any());

        assertThrows(RuntimeException.class, () -> consumer.handlePaymentCompleted("{}"));
    }

    @Test
    void ignoresBlankPayloadWithoutCallingService() {
        assertDoesNotThrow(() -> consumer.handlePaymentCompleted(""));
        verifyNoInteractions(bookingService);
    }
}
