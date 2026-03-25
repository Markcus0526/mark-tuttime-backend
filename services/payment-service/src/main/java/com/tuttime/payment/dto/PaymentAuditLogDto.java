package com.tuttime.payment.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAuditLogDto {
	private Long id;
	private Long paymentId;
	private String eventType;
	private String eventData;
	private OffsetDateTime createdAt;
	// getters and setters
}