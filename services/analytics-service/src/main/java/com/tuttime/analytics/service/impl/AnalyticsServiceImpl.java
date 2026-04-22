package com.tuttime.analytics.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.analytics.dto.AdminDto;
import com.tuttime.analytics.entity.*;
import com.tuttime.analytics.repository.*;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.event.BookingEvent;
import com.tuttime.common.event.PaymentEvent;
import com.tuttime.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.tuttime.analytics.constants.AnalyticsConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

	private final SystemEventRepository systemEventsRepository;
	private final UserReliabilityRepository reliabilityRepository;
	private final BranchYieldManagementRepository yieldRepository;
	private final FinancialLedgerRepository financialLedgerRepository;
	private final BranchPerformanceStatRepository branchPerformanceStatRepository;
	private final ObjectMapper objectMapper;

	@Override
	public List<BranchYieldManagement> getWeeklyHeatmap(UUID branchId) {
		// Queries the branch_yield_managements table for the next 7 days
		return yieldRepository.findAllByIdBranchIdOrderByIdTimeSlotAsc(branchId);
	}

	@Override
	public FinancialLedger getDailyFinancials(UUID branchId, LocalDate date) {
		return financialLedgerRepository.findByBranchIdAndReportDate(branchId, date)
				.orElseThrow(() -> new InvalidRequestException("No data for this date"));
	}

	@Override
	public AdminDto.GlobalMetricsDto calculateGlobalMetrics() {
		BigDecimal gmv = financialLedgerRepository.sumTotalGmv();
		BigDecimal commission = financialLedgerRepository.sumTotalCommission();
		Double yield = yieldRepository.calculateAveragePlatformOccupancy();

		return AdminDto.GlobalMetricsDto.builder()
				.totalGmv(gmv != null ? gmv : BigDecimal.ZERO)
				.totalCommission(commission != null ? commission : BigDecimal.ZERO)
				.activeBranchesCount(financialLedgerRepository.countDistinctBranchId())
				.platformYieldPercentage(yield != null ? yield : 0.0)
				.build();
	}

	@Override
	public BranchPerformanceStat getBranchStats(UUID branchId) {
		return branchPerformanceStatRepository.findById(branchId)
				.orElseGet(() -> {
					// If no stats exist yet, return a default empty object
					return BranchPerformanceStat.builder()
							.branchId(branchId)
							.avgPrepTimeSec(0)
							.posSyncHealthPct(BigDecimal.valueOf(100.00))
							.avgGuestRating(BigDecimal.ZERO)
							.build();
				});
	}

	@Override
	@Transactional
	public void processEvent(String source, String type, UUID branchId, Object payload) {
		// 1. PERSIST: Save raw event to system_events (Fact Table)
		SystemEvent eventLog = SystemEvent.builder()
				.serviceSource(source)
				.eventType(type)
				.branchId(branchId)
				.payload(objectMapper.valueToTree(payload))
				.occurredAt(OffsetDateTime.now())
				.build();

		systemEventsRepository.save(eventLog);

		// 2. INTELLIGENCE: Route to specific "Full Support" logic
		switch (source) {
			case SOURCE_BOOKING_SERVICE -> {
				switch (type) {
					case EVENT_BOOKING_CREATED -> handleOccupancyChange(branchId, payload, true);
					case EVENT_BOOKING_CANCELED -> handleOccupancyChange(branchId, payload, false);
					case EVENT_BOOKING_COMPLETED, EVENT_BOOKING_NO_SHOW -> handleReliabilityUpdate(type, payload);
					default -> log.debug("Event type {} logged but no further processing required", type);
				}
			}
			case SOURCE_PAYMENT_SERVICE -> {
				switch (type) {
					case EVENT_PAYMENT_SUCCESS -> handleFinancialClearing(branchId, payload);
//					case EVENT_PAYMENT_REFUNDED -> handleFinancialClearing(branchId, payload);
					default -> log.debug("Event type {} logged but no further processing required", type);
				}
			}
			case SOURCE_POS_SERVICE -> {
				switch (type) {
//					case EVENT_POS_APPROVED -> handleFinancialClearing(branchId, payload);
					default -> log.debug("Event type {} logged but no further processing required", type);
				}
			}
			case SOURCE_REVIEW_SERVICE -> {
				switch (type) {
//					case EVENT_REVIEW_APPROVED -> handleFinancialClearing(branchId, payload);
					default -> log.debug("Event type {} logged but no further processing required", type);
				}
			}
		}
	}

	private void determineYieldStrategy(BranchYieldManagement stats) {
		double rate = stats.getOccupancyRate().doubleValue();

		if (rate < 0.30) {
			stats.setDemandLevel("LOW");
			stats.setRecommendedDiscountPct(0.40f); // 40% discount for "Dead Hours"
		} else if (rate < 0.70) {
			stats.setDemandLevel("MEDIUM");
			stats.setRecommendedDiscountPct(0.20f); // 20% discount
		} else {
			stats.setDemandLevel("HIGH");
			stats.setRecommendedDiscountPct(0.0f);  // No discount for peak hours
		}
	}

	private BranchYieldManagement createInitialSlot(UUID branchId, OffsetDateTime slot) {
		// In a full implementation, you would call RestaurantClient gRPC here to get real capacity
		int defaultCapacity = 50;

		return BranchYieldManagement.builder()
				.id(new BranchYieldManagement.BranchSlotId(branchId, slot))
				.totalCapacity(defaultCapacity)
				.currentBookings(0)
				.occupancyRate(BigDecimal.ZERO)
				.build();
	}

	private void handleOccupancyChange(UUID branchId, Object payload, boolean isAdding) {
		// 1. Extract Data (Cast payload to your BookingEvent DTO)
		BookingEvent.BookingStatusUpdatedEvent event = (BookingEvent.BookingStatusUpdatedEvent) payload;
		OffsetDateTime bookingTime = event.bookingTime();

		// 2. Normalize Time to 15-minute Slots (e.g., 19:07 -> 19:00)
		OffsetDateTime slotTime = bookingTime.withMinute((bookingTime.getMinute() / 15) * 15)
				.withSecond(0).withNano(0);

		BranchYieldManagement.BranchSlotId slotId = new BranchYieldManagement.BranchSlotId(branchId, slotTime);

		// 3. Fetch or Initialize Slot Metrics
		BranchYieldManagement stat = yieldRepository.findById(slotId)
				.orElseGet(() -> {
					BranchYieldManagement newSlot = createNewSlot(branchId, slotTime);
					return yieldRepository.save(newSlot); // Persist immediately
				});

		// 4. Update Current Bookings (Atomic adjustment)
		int adjustment = isAdding ? 1 : -1; // We track occupied TABLES, not just guests
		stat.setCurrentBookings(Math.max(0, stat.getCurrentBookings() + adjustment));

		// 5. Calculate Occupancy Rate & Apply Yield Strategy
		if (stat.getTotalCapacity() > 0) {
			BigDecimal rate = BigDecimal.valueOf(stat.getCurrentBookings())
					.divide(BigDecimal.valueOf(stat.getTotalCapacity()), 2, RoundingMode.HALF_UP);

			stat.setOccupancyRate(rate);
			applyYieldStrategy(stat);
		}

		yieldRepository.save(stat);
		log.info("Branch {} slot {} updated: Rate {}%, Demand {}",
				branchId, slotTime, stat.getOccupancyRate().multiply(BigDecimal.valueOf(100)), stat.getDemandLevel());
	}

	/**
	 * The "Brain" of the Dynamic Discounting system.
	 * This directly solves the "Market Problem" of under-occupied hours.
	 */
	private void applyYieldStrategy(BranchYieldManagement stats) {
		double rate = stats.getOccupancyRate().doubleValue();

		if (rate < 0.30) {
			stats.setDemandLevel("LOW");
			stats.setRecommendedDiscountPct(0.40f); // -40% for "Dead Hours"
		} else if (rate < 0.70) {
			stats.setDemandLevel("MEDIUM");
			stats.setRecommendedDiscountPct(0.20f); // -20% for standard hours
		} else {
			stats.setDemandLevel("HIGH");
			stats.setRecommendedDiscountPct(0.0f);  // Peak hour: Full Price
		}
	}


	private void handleReliabilityUpdate(String type, Object payload) {
		// 1. Extract User ID (Cast to your BookingEvent DTO)
		BookingEvent.BookingStatusUpdatedEvent event = (BookingEvent.BookingStatusUpdatedEvent) payload;
		UUID userId = event.userId();

		if (userId == null) {
			log.warn("Reliability update skipped: No userId found in payload for event {}", type);
			return;
		}

		// 2. Fetch current stats or initialize a new record for a first-time guest
		UserReliabilityIndex stats = reliabilityRepository.findById(userId)
				.orElse(UserReliabilityIndex.builder()
						.userId(userId)
						.reliabilityScore(50) // Starting baseline score
						.totalBookings(0)
						.noShowCount(0)
						.lateCancelCount(0)
						.riskLevel("STANDARD")
						.build());

		// 3. Update Metrics & Apply Penalties/Rewards
		stats.setTotalBookings(stats.getTotalBookings() + 1);

		switch (type) {
			case EVENT_BOOKING_COMPLETED -> {
				// Reward successful attendance
				stats.setReliabilityScore(Math.min(100, stats.getReliabilityScore() + 2));
				log.debug("User {} completed booking. Score improved.", userId);
			}
			case EVENT_BOOKING_NO_SHOW -> {
				// Severe penalty for no-shows (The primary market pain point)
				stats.setNoShowCount(stats.getNoShowCount() + 1);
				stats.setReliabilityScore(Math.max(0, stats.getReliabilityScore() - 20));
				log.warn("User {} marked as NO_SHOW. Score penalized -20.", userId);
			}
			case EVENT_BOOKING_CANCELED -> {
				// Moderate penalty for late cancellations
				stats.setLateCancelCount(stats.getLateCancelCount() + 1);
				stats.setReliabilityScore(Math.max(0, stats.getReliabilityScore() - 5));
			}
		}

		// 4. Update Risk Level based on New Score
		// Thresholds used by gRPC to decide "Requires Full Prepay"
		if (stats.getReliabilityScore() < 20) {
			stats.setRiskLevel("CRITICAL");
		} else if (stats.getReliabilityScore() < 40) {
			stats.setRiskLevel("HIGH_RISK");
		} else {
			stats.setRiskLevel("STANDARD");
		}

		// 5. Finalize update
		reliabilityRepository.save(stats);
	}


	private void handleFinancialClearing(UUID branchId, Object payload) {
		// 1. Extract Payment Details (Cast to your PaymentEvent DTO)
		// Assuming payment includes: amount, currency, and the applied commission rate
		PaymentEvent.PaymentCompletedEvent event = (PaymentEvent.PaymentCompletedEvent) payload;
		BigDecimal amount = event.amount();
		BigDecimal commissionRate = event.commissionRate(); // e.g., 0.10 for 10%
		LocalDate reportDate = LocalDate.now(); // We group financials by day

		// 2. Fetch or Initialize the Daily Ledger for this Branch
		FinancialLedger ledger = financialLedgerRepository
				.findByBranchIdAndReportDate(branchId, reportDate)
				.orElse(FinancialLedger.builder()
						.branchId(branchId)
						.reportDate(reportDate)
						.totalGmv(BigDecimal.ZERO)
						.commissionEarned(BigDecimal.ZERO)
						.payoutDueRestaurant(BigDecimal.ZERO)
						.refundVolume(BigDecimal.ZERO)
						.build());

		// 3. Perform Calculations (Unit Economics)
		BigDecimal commission = amount.multiply(commissionRate)
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal netPayout = amount.subtract(commission);

		// 4. Update Cumulative Daily Totals
		ledger.setTotalGmv(ledger.getTotalGmv().add(amount));
		ledger.setCommissionEarned(ledger.getCommissionEarned().add(commission));
		ledger.setPayoutDueRestaurant(ledger.getPayoutDueRestaurant().add(netPayout));

		// 5. Finalize update
		financialLedgerRepository.save(ledger);

		log.info("Financials updated for Branch {}: GMV +{}, Commission +{}",
				branchId, amount, commission);
	}

	@Override
	public UserReliabilityIndex getUserReliability(UUID userId) {
		return reliabilityRepository.findById(userId).orElse(null);
	}

	/**
	 * Helper method to initialize a new yield management slot.
	 * This is called by the occupancy handler when a booking occurs in a new time slot.
	 */
	private BranchYieldManagement createNewSlot(UUID branchId, OffsetDateTime slotTime) {
		// In a "Full Support" system, you would ideally fetch the physical
		// capacity of this specific branch via a gRPC call to the Restaurant Service.
		// For now, we use a default capacity (e.g., 50 tables).
		int branchCapacity = 50;

		return BranchYieldManagement.builder()
				.id(new BranchYieldManagement.BranchSlotId(branchId, slotTime))
				.totalCapacity(branchCapacity)
				.currentBookings(0)
				.occupancyRate(BigDecimal.ZERO)
				.demandLevel("LOW") // New slots start with low demand
				.recommendedDiscountPct(0.40f) // Default -40% for empty slots
				.build();
	}
}
