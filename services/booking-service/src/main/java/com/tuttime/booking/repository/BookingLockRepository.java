package com.tuttime.booking.repository;

import com.tuttime.booking.entity.BookingLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingLockRepository extends JpaRepository<BookingLock, UUID> {
}