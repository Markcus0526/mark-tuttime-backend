package com.tuttime.restaurant.dto;

import java.time.LocalDateTime;

public class TableAvailabilityRequest {
  private Long branchId;
  private LocalDateTime bookingTime;
  private int numGuests;

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public LocalDateTime getBookingTime() {
    return bookingTime;
  }

  public void setBookingTime(LocalDateTime bookingTime) {
    this.bookingTime = bookingTime;
  }

  public int getNumGuests() {
    return numGuests;
  }

  public void setNumGuests(int numGuests) {
    this.numGuests = numGuests;
  }
}
