package com.tuttime.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "booking_tables", schema = "booking_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingTable {

	@EmbeddedId
	private BookingTableId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("bookingId")
	@JoinColumn(name = "booking_id", nullable = false)
	private Booking booking;

	// No @ManyToOne to Table (from restaurant service) – not available in this service.
	// The tableId is just a logical reference; you cannot have a JPA relation across services.

	// Optional extra fields
	@Column(name = "assigned_at", nullable = false)
	@Builder.Default
	private OffsetDateTime assignedAt = OffsetDateTime.now();

	@Column(name = "notes")
	private String notes;
}