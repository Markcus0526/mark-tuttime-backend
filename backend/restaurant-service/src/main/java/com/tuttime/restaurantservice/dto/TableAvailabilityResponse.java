package com.tuttime.restaurantservice.dto;

import java.util.List;

public class TableAvailabilityResponse {
  private List<Long> availableTableIds;

  public List<Long> getAvailableTableIds() {
    return availableTableIds;
  }

  public void setAvailableTableIds(List<Long> availableTableIds) {
    this.availableTableIds = availableTableIds;
  }
}
