package com.tuttime.paymentservice.service;

import lombok.Data;
import org.springframework.stereotype.Service;

@Service
@Data
public class RefundPolicyService {
  // Configurable by restaurant
  private int fullRefundMinutes = 60;
  private int partialRefundMinutes = 30;
  private BigDecimal partialRefundPercentage = new BigDecimal("0.5");
  private BigDecimal noShowRefundPercentage = new BigDecimal("0.3");
}
