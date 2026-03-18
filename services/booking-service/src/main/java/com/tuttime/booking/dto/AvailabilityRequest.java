package com.tuttime.booking.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AvailabilityRequest {
  private Long branchId;
  private LocalDateTime slotStart; // 15min slot
  private int numGuests;
}
