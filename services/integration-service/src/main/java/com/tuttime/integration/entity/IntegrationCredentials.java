package com.tuttime.integration.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "integration_credentials")
public class IntegrationCredentials {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long restaurantId;

  @Enumerated(EnumType.STRING)
  private PosSystem posSystem;

  private String apiUrl;

  private String apiKey;

  private String apiToken;

  private LocalDateTime lastSync;

  private LocalDateTime createdAt = LocalDateTime.now();

  public enum PosSystem {
    IIKO, RKEEPER
  }
}
