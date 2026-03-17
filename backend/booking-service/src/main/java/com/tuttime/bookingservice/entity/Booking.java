package com.tuttime.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  private Long restaurantId;

  private Long branchId;

  private Long tableId;

  private String status = "PENDING";

  private LocalDateTime bookingTime;

  private int numGuests;

  private int prepTime;

  private BigDecimal totalDiscount = BigDecimal.ZERO;

  private LocalDateTime createdAt = LocalDateTime.now();

}
