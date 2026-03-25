package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.BookingBoostResponse;
import com.tuttime.admin.dto.RentalBoostResponse;
import com.tuttime.admin.dto.VideoBoostResponse;
import com.tuttime.admin.rest.client.MiningServiceRestClient;
import com.tuttime.admin.service.AdminBoostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Admin Boost Service Implementation
 *
 * <p>This service integrates with the mining-service to retrieve boost data.
 * The mining-service manages booking and rental boosts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBoostServiceImpl implements AdminBoostService {

	private final MiningServiceRestClient miningServiceRestClient;

	@Override
	public Page<BookingBoostResponse> listBookingBoosts(Pageable pageable, Long userId) {
		log.info("Listing booking boosts - page: {}, size: {}, userId: {}",
				pageable.getPageNumber(), pageable.getPageSize(), userId);

		try {
			return miningServiceRestClient.listBookingBoosts(pageable, userId);
		} catch (Exception e) {
			log.warn("Mining-service unavailable when listing booking boosts, returning empty page. Error: {}", e.getMessage());
			return new PageImpl<>(List.of(), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), 0L);
		}
	}

	@Override
	public BookingBoostResponse getBookingBoost(Long id) {
		log.info("Getting booking boost: {}", id);

		try {
			return miningServiceRestClient.getBookingBoost(id);
		} catch (Exception e) {
			log.warn("Mining-service unavailable when getting booking boost id={}, returning null. Error: {}", id, e.getMessage());
			return null;
		}
	}

	@Override
	public Page<RentalBoostResponse> listRentalBoosts(Pageable pageable, Long userId) {
		log.info("Listing rental boosts - page: {}, size: {}, userId: {}",
				pageable.getPageNumber(), pageable.getPageSize(), userId);

		try {
			return miningServiceRestClient.listRentalBoosts(pageable, userId);
		} catch (Exception e) {
			log.warn("Mining-service unavailable when listing rental boosts, returning empty page. Error: {}", e.getMessage());
			return new PageImpl<>(List.of(), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), 0L);
		}
	}

	@Override
	public RentalBoostResponse getRentalBoost(Long id) {
		log.info("Getting rental boost: {}", id);

		try {
			return miningServiceRestClient.getRentalBoost(id);
		} catch (Exception e) {
			log.warn("Mining-service unavailable when getting rental boost id={}, returning null. Error: {}", id, e.getMessage());
			return null;
		}
	}

	@Override
	public Page<VideoBoostResponse> listVideoBoosts(Pageable pageable, Long userId) {
		log.info("Listing video boosts - page: {}, size: {}, userId: {}",
				pageable.getPageNumber(), pageable.getPageSize(), userId);

		try {
			return miningServiceRestClient.listVideoBoosts(pageable, userId);
		} catch (Exception e) {
			log.warn("Mining-service unavailable when listing video boosts, returning empty page. Error: {}", e.getMessage());
			return new PageImpl<>(List.of(), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), 0L);
		}
	}

	@Override
	public VideoBoostResponse getVideoBoost(Long id) {
		log.info("Getting video boost: {}", id);

		try {
			return miningServiceRestClient.getVideoBoost(id);
		} catch (Exception e) {
			log.warn("Mining-service unavailable when getting video boost id={}, returning null. Error: {}", id, e.getMessage());
			return null;
		}
	}

	@Override
	public List<Object> getBoostsByUserId(Long userId) {
		log.info("Getting all boosts for user: {}", userId);

		try {
			return miningServiceRestClient.getBoostsByUserId(userId);
		} catch (Exception e) {
			log.warn("Mining-service unavailable when getting boosts for userId={}, returning empty list. Error: {}", userId, e.getMessage());
			return List.of();
		}
	}
}

