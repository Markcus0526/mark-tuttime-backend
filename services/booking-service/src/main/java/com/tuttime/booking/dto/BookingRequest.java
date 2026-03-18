package com.tuttime.booking.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingRequest {
  private Long userId;
  private Long restaurantId;
  private Long branchId;
  private Long tableId;
  private LocalDateTime bookingTime;
  private int numGuests;
  private int prepTime;
}
