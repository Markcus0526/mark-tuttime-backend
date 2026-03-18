package com.tuttime.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long bookingId;

  private String status = "CREATED";

  private BigDecimal totalAmount;

  private BigDecimal paidAmount;

}
