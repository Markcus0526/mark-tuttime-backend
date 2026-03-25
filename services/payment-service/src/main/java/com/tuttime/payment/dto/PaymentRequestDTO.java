package com.tuttime.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDTO {

	@NotNull
	private Long bookingId;

	@NotNull
	@DecimalMin(value = "0.01", message = "Amount must be greater than zero")
	private BigDecimal amount;

	@NotBlank
	@Size(max = 10)
	private String currency;

	@NotBlank
	@Size(max = 50)
	private String paymentMethod;

	@Size(max = 255)
	private String paymentIntentId;

	@Size(max = 500)
	private String description;
}