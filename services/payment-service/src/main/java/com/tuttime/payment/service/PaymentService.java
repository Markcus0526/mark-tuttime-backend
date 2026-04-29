package com.tuttime.payment.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {

	/**
	 * Initializes a payment session for the Mobile App.
	 */
	String initiatePaymentLifecycle(UUID bookingId, UUID userId, UUID branchId, BigDecimal amount);

	/**
	 * Synchronizes local database with asynchronous provider notifications.
	 */
	void synchronizeTransactionState(String rawPayload);

	/**
	 * Fetches current payment status for mobile polling.
	 */
	String fetchPaymentStatus(UUID bookingId);

	/**
	 * Admin Panel: Executes a refund for a processed payment.
	 */
	void processAdministrativeRefund(UUID paymentId, BigDecimal amount, String reason);

	void testManualPaymentSuccess(UUID bookingId, String stripePaymentIntentId);
}
