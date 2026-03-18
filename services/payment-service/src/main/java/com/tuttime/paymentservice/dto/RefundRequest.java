package com.tuttime.paymentservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundRequest {
  private BigDecimal amount;
  private String reason;
}
