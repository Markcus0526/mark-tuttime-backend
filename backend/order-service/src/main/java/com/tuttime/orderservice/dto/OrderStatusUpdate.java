package com.tuttime.orderservice.dto;

import lombok.Data;

@Data
public class OrderStatusUpdate {
  private String status;
  private String cancelReason;
}
