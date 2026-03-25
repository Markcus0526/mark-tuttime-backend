package com.tuttime.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequestDTO {

	@NotNull
	private Long paymentId;

	@NotNull
	@DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
	private BigDecimal amount;

	@Size(max = 255)
	private String reason;
}
