package com.tuttime.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "modifiers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modifier {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(precision = 6, scale = 2)
  private BigDecimal price = BigDecimal.ZERO;

  private boolean required = false;

  @ManyToOne
  @JoinColumn(name = "item_id")
  private MenuItem item;

}
