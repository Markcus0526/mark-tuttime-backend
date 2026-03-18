package com.tuttime.payment.dto;

import lombok.Data;

@Data
public class PaymentResponse {
  private Long id;
  private String status;
  private String transactionId;
}
