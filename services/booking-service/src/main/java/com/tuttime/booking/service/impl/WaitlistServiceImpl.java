package com.tuttime.booking.service.impl;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.dto.WaitlistDto;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.WaitlistEntry;
import com.tuttime.booking.entity.enums.WaitlistStatus;
import com.tuttime.booking.grpc.client.RestaurantGrpcClient;
import com.tuttime.booking.mapper.BookingMapper;
import com.tuttime.booking.repository.WaitlistRepository;
import com.tuttime.booking.service.BookingService;
import com.tuttime.booking.service.WaitlistService;
import com.tuttime.common.enums.UserRoleKind;
import com.tuttime.common.event.proto.FindAvailableTableResponse;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistServiceImpl implements WaitlistService {

	private final WaitlistRepository waitlistRepository;
	private final BookingService bookingService;
	private final BookingMapper bookingMapper;
	private final RestaurantGrpcClient restaurantGrpcClient;

	@Override
	@Transactional
	public WaitlistDto.WaitlistResponse addToWaitlist(WaitlistDto.WaitlistRequest request,
	                                                  UUID currentUserId,
	                                                  String userRoles) {
		// Use authenticated user ID (ignore request.userId for security)
		UUID userId = currentUserId;

		// Check if user already waiting at this branch
		boolean alreadyWaiting = waitlistRepository.existsByUserIdAndBranchIdAndStatus(
				userId, request.branchId(), WaitlistStatus.WAITING);

		if (alreadyWaiting) {
			throw new ConflictException("User is already on the waitlist for this branch.");
		}

		WaitlistEntry entry = WaitlistEntry.builder()
				.userId(userId)
				.branchId(request.branchId())
				.zoneId(request.zoneId())
				.partySize(request.partySize())
				.joinedAt(OffsetDateTime.now())
				.status(WaitlistStatus.WAITING)
				.build();

		WaitlistEntry saved = waitlistRepository.save(entry);
		log.info("User {} joined waitlist at branch {}", userId, request.branchId());

		return mapToResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public WaitlistDto.WaitlistResponse getWaitlistEntry(UUID id, UUID currentUserId, String userRoles) {
		WaitlistEntry entry = waitlistRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Waitlist entry not found"));

		// Security: allow admin/owner/manager or the owner of the entry
		if (!isAdminOrOwnerOrManager(userRoles) && !entry.getUserId().equals(currentUserId)) {
			throw new ForbiddenException("You are not authorized to view this waitlist entry");
		}

		return mapToResponse(entry);
	}

	@Override
	@Transactional
	public void leaveWaitlist(UUID id, UUID currentUserId, String userRoles) {
		WaitlistEntry entry = waitlistRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Waitlist entry not found"));

		// Security: same as above
		if (!isAdminOrOwnerOrManager(userRoles) && !entry.getUserId().equals(currentUserId)) {
			throw new ForbiddenException("You are not authorized to remove this waitlist entry");
		}

		entry.setStatus(WaitlistStatus.CANCELED);
		waitlistRepository.save(entry);
		log.info("User {} left the waitlist (entry {})", entry.getUserId(), id);
	}

	@Override
	@Transactional
	public void updateWaitlistStatus(UUID id, WaitlistStatus newStatus, UUID currentUserId, String userRoles) {
		// This method should be accessible only to admins/managers
		if (!isAdminOrOwnerOrManager(userRoles)) {
			throw new ForbiddenException("Only administrators or restaurant staff can update waitlist status.");
		}

		WaitlistEntry entry = waitlistRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Waitlist entry not found"));

		entry.setStatus(newStatus);
		if (newStatus == WaitlistStatus.NOTIFIED) {
			entry.setNotifiedAt(OffsetDateTime.now());
		}
		waitlistRepository.save(entry);
		log.info("Waitlist entry {} status updated to {}", id, newStatus);
	}

	@Override
	@Transactional
	public void notifyNextWaiting(UUID branchId, UUID zoneId, int partySize) {
		// Find the earliest waiting entry for this branch and zone (party size can be fuzzy)
		Optional<WaitlistEntry> next = waitlistRepository.findFirstByBranchIdAndZoneIdAndStatusOrderByJoinedAtAsc(
				branchId, zoneId, WaitlistStatus.WAITING);
		if (next.isEmpty()) {
			log.debug("No waiting users for branch {} zone {}", branchId, zoneId);
			return;
		}
		WaitlistEntry entry = next.get();
		entry.setStatus(WaitlistStatus.NOTIFIED);
		entry.setNotifiedAt(OffsetDateTime.now());
		waitlistRepository.save(entry);
		log.info("Notified user {} for waitlist entry {}", entry.getUserId(), entry.getId());
		// TODO: send push notification via Notification Service (gRPC or Kafka)
	}

	@Override
	@Transactional
	public BookingDto.BookingResponse claimWaitlistEntry(UUID entryId,
	                                                     BookingDto.BookingRequest bookingRequest,
	                                                     UUID currentUserId,
	                                                     String userRoles) {
		// 1. Fetch waitlist entry
		WaitlistEntry entry = waitlistRepository.findById(entryId)
				.orElseThrow(() -> new InvalidRequestException("Waitlist entry not found"));

		// 2. Security check
		if (!entry.getUserId().equals(currentUserId) && !isAdminOrOwnerOrManager(userRoles)) {
			throw new ForbiddenException("Not authorized to claim this waitlist entry");
		}

		// 3. Must be in NOTIFIED state
		if (entry.getStatus() != WaitlistStatus.NOTIFIED) {
			throw new InvalidRequestException("Waitlist entry is not in NOTIFIED state");
		}

		// 4. Find an available table (immediate seating)
		FindAvailableTableResponse tableDetail = restaurantGrpcClient.findAvailableTable(
				entry.getBranchId(),
				entry.getZoneId(),
				entry.getPartySize(),
				OffsetDateTime.now()
		);
		if (!tableDetail.getAvailable()) {
			throw new InvalidRequestException("No table currently available for your party size. Please try again later.");
		}

		// 5. Build BookingRequest – all fields are mandatory except those with defaults
		List<BookingDto.BookingRequest.PreOrderItemRequest> preOrderItems = bookingRequest.preOrderItems() != null
				? bookingRequest.preOrderItems()
				: Collections.emptyList();

		BookingDto.BookingRequest mergedRequest = new BookingDto.BookingRequest(
				currentUserId,                                           // userId
				UUID.fromString(tableDetail.getTableId()),               // tableId
				OffsetDateTime.now(),                                    // bookingDatetime
				bookingRequest.duration() != null ? bookingRequest.duration() : 90, // duration (default 90 min)
				entry.getPartySize(),                                    // partySize
				bookingRequest.leadTimeMinutes() != null ? bookingRequest.leadTimeMinutes() : 0,
				bookingRequest.gracePeriodMins() != null ? bookingRequest.gracePeriodMins() : 15,
				bookingRequest.guestName(),
				bookingRequest.guestPhone(),
				bookingRequest.specialRequest(),
				preOrderItems
		);

		// 6. Create the booking (internal method sets status to CONFIRMED)
		Booking booking = bookingService.createBookingFromWaitlist(mergedRequest, currentUserId, userRoles);

		// 7. Update waitlist entry to CONVERTED
		entry.setStatus(WaitlistStatus.CONVERTED);
		waitlistRepository.save(entry);

		log.info("Waitlist entry {} claimed, created booking {}", entryId, booking.getId());

		// 8. Return response using mapper
		return bookingMapper.toResponse(booking);
	}

	// ---------- Helper methods ----------
	private WaitlistDto.WaitlistResponse mapToResponse(WaitlistEntry entity) {
		long position = 0;
		if (entity.getStatus() == WaitlistStatus.WAITING) {
			// Count entries before this one (by joinedAt) in the same branch and zone
			position = waitlistRepository.countByBranchIdAndZoneIdAndStatusAndJoinedAtBefore(
					entity.getBranchId(),
					entity.getZoneId(),
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

	private boolean isAdminOrOwnerOrManager(String userRoles) {
		if (userRoles == null) return false;
		List<String> roles = Arrays.asList(userRoles.split(","));
		return roles.contains(UserRoleKind.ADMIN.name()) ||
				roles.contains(UserRoleKind.OWNER.name()) ||
				roles.contains(UserRoleKind.MANAGER.name());
	}
}