package com.tuttime.payment.dto;

import com.tuttime.payment.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PaymentResponseDTO {

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
}