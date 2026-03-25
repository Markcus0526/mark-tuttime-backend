package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for rental boost details
 *
 * <p>Rental boosts are for apartment owners (separate from booking boosts which are for renters).
 * Each confirmed rental provides a +2.0 AP/hour boost for 15 days.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalBoostResponse {

	private Long boostId;
	private Long userId; // Apartment owner
	private Long rentalId; // References booking_integration_service.rentals.rental_id
	private BigDecimal boostAmount; // AP/hour boost amount
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime startedAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime expiresAt;
	private Boolean isActive;
	private Boolean isExpired; // Computed: expiresAt < now
	private Long daysRemaining; // Computed: days until expiration
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
}

