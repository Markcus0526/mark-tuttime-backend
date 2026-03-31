package com.tuttime.payment.dto;

import com.tuttime.payment.enums.RefundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundDto {
	private Long id;
	private String refundId;
	private Long paymentId;
	private BigDecimal amount;
	private RefundStatus status;
	private String reason;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
	// getters and setters
}
