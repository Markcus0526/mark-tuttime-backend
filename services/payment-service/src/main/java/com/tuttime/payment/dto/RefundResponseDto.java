package com.tuttime.payment.dto;

import com.tuttime.payment.enums.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class RefundResponseDto {

	private Long id;
	private String refundId;
	private Long paymentId;
	private BigDecimal amount;
	private RefundStatus status;
	private String reason;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
}