package com.tuttime.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentAuditLogRequestDTO {

	@NotNull
	private Long paymentId;

	@NotBlank
	@Size(max = 100)
	private String eventType;

	private String eventData;
}