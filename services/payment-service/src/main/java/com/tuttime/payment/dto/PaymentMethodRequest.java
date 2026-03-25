package com.tuttime.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentMethodRequest {
	private String method; // "FULL_PREPAY", "DEPOSIT"
	private BigDecimal depositPercentage;
}
