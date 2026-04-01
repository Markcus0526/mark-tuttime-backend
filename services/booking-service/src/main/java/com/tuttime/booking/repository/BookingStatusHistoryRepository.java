package com.tuttime.booking.repository;

import com.tuttime.booking.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
	List<BookingStatusHistory> findAllByBookingIdOrderByChangedAtDesc(UUID bookingId);
}
