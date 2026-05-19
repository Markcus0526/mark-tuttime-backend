package com.tuttime.booking.repository;

import com.tuttime.booking.entity.BookingTable;
import com.tuttime.booking.entity.BookingTableId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingTableRepository extends JpaRepository<BookingTable, BookingTableId> {

    /**
     * Find all table assignments for a specific booking.
     *
     * @param bookingId the booking ID
     * @return list of BookingTable entities
     */
    List<BookingTable> findById_BookingId(UUID bookingId);

    /**
     * Check if a particular table is already assigned to a booking.
     *
     * @param bookingId booking ID
     * @param tableId   table ID
     * @return true if exists
     */
    boolean existsById_BookingIdAndId_TableId(UUID bookingId, UUID tableId);

    /**
     * Delete all table assignments for a given booking.
     *
     * @param bookingId the booking ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM BookingTable bt WHERE bt.id.bookingId = :bookingId")
    void deleteByBookingId(@Param("bookingId") UUID bookingId);

    /**
     * Find all bookings that include a specific table (for availability checks).
     *
     * @param tableId the table ID
     * @return list of BookingTable entries
     */
    List<BookingTable> findById_TableId(UUID tableId);

    /**
     * Count how many tables are assigned to a booking.
     *
     * @param bookingId the booking ID
     * @return count of tables
     */
    long countById_BookingId(UUID bookingId);
}