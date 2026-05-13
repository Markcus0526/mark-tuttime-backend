package com.tuttime.payment.service;

import com.tuttime.common.enums.PaymentStatus;
import com.tuttime.payment.dto.PaymentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
	boolean processAdministrativeRefund(UUID paymentId, BigDecimal amount, String reason);

	void testManualPaymentSuccess(UUID bookingId, String stripePaymentIntentId);

	Page<PaymentDto.AdminPaymentResponse> listBranchPayments(UUID branchId, PaymentStatus status,
	                                                         OffsetDateTime fromDate, OffsetDateTime toDate,
	                                                         String query, Pageable pageable,
	                                                         UUID currentUserId, String userRoles);

	PaymentDto.AdminPaymentDetailResponse getPaymentForAdmin(UUID paymentId, UUID currentUserId, String userRoles);

	PaymentDto.BranchPaymentSummary getBranchPaymentSummary(UUID branchId, UUID currentUserId, String userRoles);

	Page<PaymentDto.PayoutResponse> listPayouts(UUID branchId, OffsetDateTime fromDate, OffsetDateTime toDate,
	                                            Pageable pageable, UUID currentUserId, String userRoles);
}
