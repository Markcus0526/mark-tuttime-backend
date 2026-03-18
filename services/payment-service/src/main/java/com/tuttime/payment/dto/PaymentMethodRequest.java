package com.tuttime.payment.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentMethodRequest {
  private String method; // "FULL_PREPAY", "DEPOSIT"
  private BigDecimal depositPercentage;
}
