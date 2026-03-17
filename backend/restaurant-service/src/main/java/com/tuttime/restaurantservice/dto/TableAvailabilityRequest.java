package com.tuttime.restaurantservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TableAvailabilityRequest {
  private Long branchId;
  private LocalDateTime bookingTime;
  private int numGuests;
}
