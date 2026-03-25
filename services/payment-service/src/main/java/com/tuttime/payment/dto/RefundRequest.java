package com.tuttime.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
	private BigDecimal amount;
	private String reason;
}
