package com.tuttime.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "booking_id")
  private Long bookingId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private OrderStatus status = OrderStatus.CREATED;

  @Column(name = "items", columnDefinition = "jsonb")
  private String itemsJson;

  @Column(name = "total_amount")
  private BigDecimal totalAmount;

  @Column(name = "paid_amount")
  private BigDecimal paidAmount;

  @Column(name = "cancel_reason")
  private String cancelReason;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now();
}
