package com.tuttime.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long orderId;

  private BigDecimal amount;

  private String currency = "RUB";

  private String status = "HELD";

  private String transactionId;

  private LocalDateTime createdAt = LocalDateTime.now();

}
