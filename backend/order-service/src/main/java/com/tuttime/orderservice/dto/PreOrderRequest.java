package com.tuttime.orderservice.dto;

import lombok.Data;
import java.util.Map;

@Data
public class PreOrderRequest {
  private Long bookingId;
  private Map<Long, Integer> menuItems; // itemId -> quantity
}
