package com.tuttime.bookingservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingResponse {
  private Long id;
  private Long userId;
  private Long restaurantId;
  private Long branchId;
  private Long tableId;
  private String status;
  private LocalDateTime bookingTime;
  private int numGuests;
  private int prepTime;
}
