package com.tuttime.booking.repository;

import com.tuttime.booking.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing {@link BookingItem} entities.
 * Corresponds to the 'booking_items' table in the 'booking_service' schema.
 */
@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {

    /**
     * Find all pre-ordered items for a specific booking.
     * Useful for displaying the order summary or calculating refunds.
     *
     * @param bookingId The ID of the parent booking
     * @return List of booking items
     */
    List<BookingItem> findByBookingId(UUID bookingId);
}
