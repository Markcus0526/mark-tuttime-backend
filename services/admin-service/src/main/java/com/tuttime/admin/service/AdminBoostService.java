package com.tuttime.admin.service;

import com.tuttime.admin.dto.BookingBoostResponse;
import com.tuttime.admin.dto.RentalBoostResponse;
import com.tuttime.admin.dto.VideoBoostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Admin Boost Service Interface
 *
 * <p>TODO: Integrate with booking-integration-service or mining-service for actual boost data.
 * Current implementation provides placeholder methods that return example data.
 */
public interface AdminBoostService {
	Page<BookingBoostResponse> listBookingBoosts(Pageable pageable, Long userId);

	BookingBoostResponse getBookingBoost(Long id);

	Page<RentalBoostResponse> listRentalBoosts(Pageable pageable, Long userId);

	RentalBoostResponse getRentalBoost(Long id);

	Page<VideoBoostResponse> listVideoBoosts(Pageable pageable, Long userId);

	VideoBoostResponse getVideoBoost(Long id);

	List<Object> getBoostsByUserId(Long userId);
}

