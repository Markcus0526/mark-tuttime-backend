package com.tuttime.bookingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
  private final BookingRepository bookingRepository;
  private final RestaurantClient restaurantClient;

  public boolean checkAvailability(Long branchId, LocalDateTime slot, int numGuests) {
    LocalDateTime start = slot.withMinute(slot.getMinute() / 15 * 15).withSecond(0).withNano(0);
    LocalDateTime end = start.plusMinutes(15);

    long bookedSeats = bookingRepository.countByBranchIdAndBookingTimeBetween(branchId, start, end);

    List<TableAvailabilityDto> availTables = restaurantClient.getAvailableTables(branchId, start);
    long availSeats = availTables.stream().filter(TableAvailabilityDto::isAvailable).mapToInt(TableAvailabilityDto::getSeats).sum();

    return (availSeats - bookedSeats) >= numGuests;
  }

  public List<Long> getAvailableTables(Long branchId, LocalDateTime slot, int numGuests) {
    LocalDateTime start = slot.withMinute(slot.getMinute() / 15 * 15).withSecond(0).withNano(0);
    return restaurantClient.getAvailableTables(branchId, start).stream()
      .filter(t -> t.isAvailable())
      .map(TableAvailabilityDto::getTableId)
      .collect(Collectors.toList());
  }
}
