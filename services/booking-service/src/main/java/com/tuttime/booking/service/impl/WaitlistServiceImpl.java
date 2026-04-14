package com.tuttime.booking.service.impl;

import com.tuttime.booking.dto.WaitlistDto;
import com.tuttime.booking.entity.WaitlistEntry;
import com.tuttime.booking.entity.enums.WaitlistStatus;
import com.tuttime.booking.repository.WaitlistRepository;
import com.tuttime.booking.service.WaitlistService;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WaitlistServiceImpl implements WaitlistService {

	private final WaitlistRepository waitlistRepository;

	@Override
	@Transactional
	public WaitlistDto.WaitlistResponse addToWaitlist(WaitlistDto.WaitlistRequest request) {
		// 1. Logic: Check if user is already waiting at this branch
		boolean alreadyWaiting = waitlistRepository.existsByUserIdAndBranchIdAndStatus(
				request.userId(), request.branchId(), WaitlistStatus.WAITING);

		if (alreadyWaiting) {
			throw new ConflictException("User is already on the waitlist for this branch.");
		}

		// 2. Map Record to Entity
		WaitlistEntry entry = WaitlistEntry.builder()
				.userId(request.userId())
				.branchId(request.branchId())
				.zoneId(request.zoneId())
				.partySize(request.partySize())
				.joinedAt(OffsetDateTime.now())
				.status(WaitlistStatus.WAITING)
				.build();

		WaitlistEntry saved = waitlistRepository.save(entry);
		log.info("User {} joined waitlist at branch {}", request.userId(), request.branchId());

		return mapToResponse(saved);
	}

	@Override
	public WaitlistDto.WaitlistResponse getWaitlistEntry(UUID id) {
		WaitlistEntry entry = waitlistRepository.findById(id)
				.orElseThrow(() -> new MethodArgumentNotValidException("Waitlist entry not found"));

		return mapToResponse(entry);
	}

	@Override
	@Transactional
	public void leaveWaitlist(UUID id) {
		WaitlistEntry entry = waitlistRepository.findById(id)
				.orElseThrow(() -> new MethodArgumentNotValidException("Waitlist entry not found"));

		entry.setStatus(WaitlistStatus.CANCELED);
		waitlistRepository.save(entry);
		log.info("User {} left the waitlist", entry.getUserId());
	}

	@Override
	@Transactional
	public void updateWaitlistStatus(UUID id, WaitlistStatus newStatus) {
		WaitlistEntry entry = waitlistRepository.findById(id)
				.orElseThrow(() -> new MethodArgumentNotValidException("Waitlist entry not found"));

		entry.setStatus(newStatus);
		if (newStatus == WaitlistStatus.NOTIFIED) {
			entry.setNotifiedAt(OffsetDateTime.now());
		}
		waitlistRepository.save(entry);
	}

	/**
	 * Internal helper to calculate queue position and map to Record
	 */
	private WaitlistDto.WaitlistResponse mapToResponse(WaitlistEntry entity) {
		long position = 0;
		if (entity.getStatus() == WaitlistStatus.WAITING) {
			// Count how many people joined BEFORE this entry at the same branch/zone
			position = waitlistRepository.countByBranchIdAndStatusAndJoinedAtBefore(
					entity.getBranchId(),
					WaitlistStatus.WAITING,
					entity.getJoinedAt()) + 1;
		}

		return new WaitlistDto.WaitlistResponse(
				entity.getId(),
				entity.getUserId(),
				entity.getStatus(),
				entity.getJoinedAt(),
				(int) position
		);
	}
}
