package com.tuttime.restaurantservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "restaurants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  private String description;

  private String address;

  private BigDecimal rating = BigDecimal.ZERO;

  private LocalDateTime createdAt = LocalDateTime.now();

  @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
  private List<Branch> branches;

}
