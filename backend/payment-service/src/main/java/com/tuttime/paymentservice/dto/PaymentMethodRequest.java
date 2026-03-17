package com.tuttime.paymentservice.dto;

import lombok.Data;

@Data
public class PaymentMethodRequest {
  private String method; // "FULL_PREPAY", "DEPOSIT"
  private BigDecimal depositPercentage;
}
