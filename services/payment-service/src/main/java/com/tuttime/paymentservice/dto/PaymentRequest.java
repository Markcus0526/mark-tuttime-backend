package com.tuttime.paymentservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
  private Long orderId;
  private BigDecimal amount;
}
