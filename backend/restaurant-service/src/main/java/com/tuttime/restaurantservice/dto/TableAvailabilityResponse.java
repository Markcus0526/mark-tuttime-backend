package com.tuttime.restaurantservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class TableAvailabilityResponse {
  private List<Long> availableTableIds;
}
