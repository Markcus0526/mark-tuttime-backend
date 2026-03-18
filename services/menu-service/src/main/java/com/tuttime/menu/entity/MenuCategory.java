package com.tuttime.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "menu_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  private Long restaurantId;

  private LocalDateTime createdAt = LocalDateTime.now();

  @OneToMany(mappedBy = "category")
  private List<MenuItem> items;

}
