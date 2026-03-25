package com.tuttime.booking.dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDto {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID) // Standard for modern Hibernate
	@Column(name = "booking_id", updatable = false, nullable = false)
	private UUID bookingId;

	@NotNull(message = "User ID cannot be null")
	private UUID userId;

	@NotNull(message = "Restaurant ID cannot be null")
	private UUID restaurantId;

	@NotNull(message = "Branch ID cannot be null")
	private UUID branchId;

	@NotNull(message = "Zone ID cannot be null")
	private UUID zoneId;

	@NotNull(message = "Table ID cannot be null")
	private UUID tableId;

	@NotBlank(message = "Booking status is required")
	@Size(max = 20)
	private String bookingStatus;

	@NotNull(message = "Booking datetime is required")
	private Instant bookingDatetime;

	@Min(value = 1, message = "Party size must be at least 1")
	private int partySize;

	@Min(value = 0)
	private Integer leadTimeMinutes;

	@Min(value = 0)
	private Integer gracePeriodMins;

	private Instant createdAt;
	private Instant updatedAt;

	@Size(max = 255)
	private String cancellationReason;

	private Boolean autoCancelled;
}
