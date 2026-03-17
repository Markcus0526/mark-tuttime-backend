package com.tuttime.integrationservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sync_log")
public class SyncLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long restaurantId;

  private String type;

  @Column(columnDefinition = "JSONB")
  private String posResponse;

  private String status;

  private String errorMessage;

  private LocalDateTime syncTime = LocalDateTime.now();
}
