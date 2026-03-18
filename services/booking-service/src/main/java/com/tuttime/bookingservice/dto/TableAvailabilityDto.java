package com.tuttime.bookingservice.dto;

import lombok.Data;

@Data
public class TableAvailabilityDto {
  private Long tableId;
  private String name;
  private int seats;
  private boolean available;
  private String lockedBy;
}
