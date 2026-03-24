package com.tuttime.restaurant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;

@Entity
@jakarta.persistence.Table(name = "branches", schema = "restaurant_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "branch_id")
  private Long branchId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "restaurant_id", nullable = false)
  private Restaurant restaurant;

  @Column(length = 255, nullable = false)
  private String name;

  @Column(length = 512, nullable = false)
  private String address;

  @Column(length = 20)
  private String phone;

  @Column(length = 50, nullable = false)
  private String timezone;

  @Column(columnDefinition = "geography(Point,4326)")
  @JdbcTypeCode(SqlTypes.GEOMETRY) // Modern alternative to @Type
  private Point location;

  @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private OffsetDateTime updatedAt;

  // Getters and setters omitted for brevity

}
