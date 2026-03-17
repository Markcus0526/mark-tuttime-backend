package com.tuttime.bookingservice.repository;

import com.tuttime.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
  
  @Query("SELECT COUNT(b) FROM Booking b WHERE b.branchId = :branchId AND b.bookingTime BETWEEN :start AND :end")
  long countByBranchIdAndBookingTimeBetween(@Param("branchId") Long branchId, 
      @Param("start") LocalDateTime start, 
      @Param("end") LocalDateTime end);

  @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookingTime < :time")
  List<Booking> findByStatusAndBookingTimeBefore(@Param("status") String status, @Param("time") LocalDateTime time);
}
