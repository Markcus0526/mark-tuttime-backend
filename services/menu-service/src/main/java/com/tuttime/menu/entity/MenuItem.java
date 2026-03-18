package com.tuttime.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "menu_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  private String description;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  private String imageUrl;

  private boolean isAvailable = true;

  private LocalDateTime createdAt = LocalDateTime.now();

  @ManyToOne
  @JoinColumn(name = "category_id")
  private MenuCategory category;

  @OneToMany(mappedBy = "item")
  private List<Modifier> modifiers;

}
