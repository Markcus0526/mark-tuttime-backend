package com.tuttime.booking.dto;

import lombok.Data;
import java.util.List;

@Data
public class AvailabilityResponse {
  private List<TableAvailabilityDto> availableTables;
  private boolean slotCapacityOk;
}
