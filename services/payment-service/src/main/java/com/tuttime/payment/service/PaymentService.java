package com.tuttime.payment.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
	// Used by gRPC (Booking Service)
	String createCheckoutSession(UUID bookingId, UUID userId, BigDecimal amount);

	void processRefund(UUID bookingId, BigDecimal amount, String reason);
}
