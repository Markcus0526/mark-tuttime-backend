package com.tuttime.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "events")
public class Event {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "type")
  private String type;

  @Column(name = "restaurant_id")
  private Long restaurantId;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "data", columnDefinition = "jsonb")
  private String dataJson;

  @Column(name = "timestamp")
  private LocalDateTime timestamp;
}
