package com.tuttime.bookingservice.service;

import com.tuttime.bookingservice.dto.BookingRequest;
import com.tuttime.bookingservice.dto.BookingResponse;
import com.tuttime.bookingservice.entity.Booking;
import com.tuttime.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingService {

  private final BookingRepository bookingRepository;
  private final RedisTableLockService redisTableLockService;
  private final AvailabilityService availabilityService;
  private final RedisTemplate<String, String> redisTemplate;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public BookingResponse createBooking(BookingRequest request) {
    // Availability check
    if (!availabilityService.checkAvailability(request.getBranchId(), request.getBookingTime(), request.getNumGuests())) {
      throw new RuntimeException("No availability");
    }

    // Lead time 30-60min
    long leadTime = 30;
    if (request.getBookingTime().isBefore(LocalDateTime.now().plusMinutes(leadTime))) {
      throw new RuntimeException("Too soon - min " + leadTime + "min");
    }

    // Prep time stub 20min/order size
    int prepTime = 20 + request.getNumGuests() * 5;

    // Lock Redis
    if (!redisTableLockService.lockTable(request.getTableId(), String.valueOf(request.getUserId()), Duration.ofMinutes(10))) {
      throw new RuntimeException("Table locked");
    }

    Booking booking = Booking.builder()
        .userId(request.getUserId())
        .restaurantId(request.getRestaurantId())
        .branchId(request.getBranchId())
        .tableId(request.getTableId())
        .bookingTime(request.getBookingTime())
        .numGuests(request.getNumGuests())
        .prepTime(prepTime)
        .status("PENDING_PAYMENT")
        .build();

    booking = bookingRepository.save(booking);

    // Kafka to payment/order
    kafkaTemplate.send("booking-events", "booking.created", Map.of("bookingId", booking.getId()));

    return mapToResponse(booking);
  }

  private BookingResponse mapToResponse(Booking booking) {
    BookingResponse response = new BookingResponse();
    response.setId(booking.getId());
    response.setUserId(booking.getUserId());
    response.setTableId(booking.getTableId());
    response.setStatus(booking.getStatus());
    response.setPrepTime(booking.getPrepTime());
    return response;
  }

}
