package com.tuttime.booking.service;

import com.tuttime.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoCancelScheduler {
  private final BookingRepository bookingRepository;
  private final RedisTableLockService lockService;

  @Scheduled(fixedRate = 60000) // 1min
  public void cancelGracePeriodBookings() {
    LocalDateTime cutoff = LocalDateTime.now().minus(Duration.ofMinutes(15));
    var pending = bookingRepository.findByStatusAndBookingTimeBefore("PENDING_PAYMENT", cutoff);
    pending.forEach(b -> {
      b.setStatus("CANCELLED_GRACE");
      bookingRepository.save(b);
      lockService.releaseLock(b.getTableId());
      log.info("Auto-cancelled booking {}", b.getId());
    });
  }
}
