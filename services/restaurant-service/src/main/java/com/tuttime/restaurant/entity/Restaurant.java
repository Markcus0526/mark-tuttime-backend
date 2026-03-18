package com.tuttime.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@jakarta.persistence.Table(name = "restaurants")
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

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
  private List<Branch> branches;

}
