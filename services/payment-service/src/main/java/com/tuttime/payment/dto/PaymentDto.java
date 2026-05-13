package com.tuttime.payment.dto;

import com.tuttime.common.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class PaymentDto {

	/**
	 * Immutable DTO for initiating a payment session.
	 * Used by REST controllers and internal service mapping.
	 */
	public record PaymentRequest(
			@NotNull(message = "Booking ID is required")
			UUID bookingId,

			@NotNull(message = "User ID is required")
			UUID userId,

			@NotNull(message = "Branch ID is required")
			UUID branchId,

			@NotNull(message = "Amount is required")
			@DecimalMin(value = "0.01", message = "Amount must be greater than zero")
			BigDecimal amount,

			@NotBlank(message = "Currency is required")
			String currency, // e.g., "RUB"

			String description // Optional: e.g., "Table Reservation for Branch A"
	) {
		// Compact constructor to set defaults if needed
		public PaymentRequest {
			if (currency == null || currency.isBlank()) {
				currency = "RUB";
			}
		}
	}

	// ========== ADMIN / RESTAURANT DTOs ==========

	public record AdminPaymentResponse(
			UUID paymentId,
			UUID bookingId,
			String userIdentifier,
			String userEmail,
			String userPhone,
			BigDecimal amount,
			String currency,
			PaymentStatus status,
			String paymentMethodType,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt,
			BigDecimal commission,
			BigDecimal netAmount
	) {}

	public record AdminPaymentDetailResponse(
			UUID paymentId,
			UUID bookingId,
			UUID userId,
			String userIdentifier,
			String userEmail,
			String userPhone,
			BigDecimal amount,
			String currency,
			PaymentStatus status,
			String yookassaPaymentId,
			String paymentMethodType,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt,
			List<RefundDto> refunds,
			BigDecimal totalRefundedAmount,
			BigDecimal commission,
			BigDecimal netAmount
	) {}

	public record RefundDto(
			UUID refundId,
			BigDecimal amount,
			String status,
			String reason,
			OffsetDateTime createdAt
	) {}

	public record BranchPaymentSummary(
			BigDecimal todayTotal,
			Long todayCount,
			BigDecimal weekTotal,
			Long weekCount,
			BigDecimal monthTotal,
			Long monthCount,
			BigDecimal pendingTotal,
			Long pendingCount,
			BigDecimal refundedTotal,
			Long refundedCount
	) {}

	public record PayoutResponse(
			UUID paymentId,
			OffsetDateTime updatedAt,
			BigDecimal amount,
			BigDecimal commission,
			BigDecimal netAmount,
			String yookassaPaymentId
	) {}
}
