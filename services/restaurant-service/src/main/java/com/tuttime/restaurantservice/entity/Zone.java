package com.tuttime.restaurantservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Entity
@jakarta.persistence.Table(name = "zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "branch_id")
  private Branch branch;

  private String name;

  private int capacity;

  @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL)
  private List<Table> tables;

}
