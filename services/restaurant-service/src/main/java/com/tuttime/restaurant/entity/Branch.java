package com.tuttime.restaurant.entity;

import com.tuttime.restaurant.entity.readmodel.RestaurantReadModel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Entity
@jakarta.persistence.Table(name = "branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "restaurant_id")
  private RestaurantReadModel restaurant;

  private String name;

  private String address;

  @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL)
  private List<Zone> zones;

}
