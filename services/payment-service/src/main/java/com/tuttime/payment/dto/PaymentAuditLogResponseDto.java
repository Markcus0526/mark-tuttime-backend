package com.tuttime.payment.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PaymentAuditLogResponseDto {

	private Long id;
	private Long paymentId;
	private String eventType;
	private String eventData;
	private OffsetDateTime createdAt;
}