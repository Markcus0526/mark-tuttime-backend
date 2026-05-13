package com.tuttime.booking.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BookingTableId implements Serializable {

	@Column(name = "booking_id")
	private UUID bookingId;

	@Column(name = "table_id")
	private UUID tableId;
}