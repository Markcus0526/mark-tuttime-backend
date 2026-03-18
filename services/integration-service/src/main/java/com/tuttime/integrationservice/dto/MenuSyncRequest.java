package com.tuttime.integrationservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MenuSyncRequest {
  private Long restaurantId;
  private String posId; // iiko or r_keeper orderId
  private String name;
  private String category;
  private BigDecimal price;
  private String modifiers; // JSON string
  private boolean stopList;
}
