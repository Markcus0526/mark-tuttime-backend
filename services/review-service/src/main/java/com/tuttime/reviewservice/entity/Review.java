package com.tuttime.reviewservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  private Long restaurantId;

  @Column(precision = 3, scale = 2)
  private BigDecimal rating;

  private String comment;

  private LocalDateTime createdAt = LocalDateTime.now();

}
