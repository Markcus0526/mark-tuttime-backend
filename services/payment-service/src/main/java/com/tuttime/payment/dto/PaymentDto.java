package com.tuttime.payment.dto;

import com.tuttime.payment.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {
	private Long id;
	private String paymentIntentId;
	private Long bookingId;
	private BigDecimal amount;
	private String currency;
	private String paymentMethod;
	private PaymentStatus status;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
	private String description;
	private String receiptUrl;
	private Set<RefundDto> refunds;
	private Set<PaymentAuditLogDto> auditLogs;
	// getters and setters
}
