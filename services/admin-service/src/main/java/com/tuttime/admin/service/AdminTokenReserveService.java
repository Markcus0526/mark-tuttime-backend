package com.tuttime.admin.service;

import com.tuttime.admin.dto.ReserveAlarmRequest;
import com.tuttime.admin.dto.ReserveAlarmResponse;
import com.tuttime.admin.dto.TokenReserveResponse;

import java.util.List;

/**
 * Admin Token Reserve Service Interface
 */
public interface AdminTokenReserveService {

	/**
	 * Get aggregated reserves across all networks
	 * <p>
	 * TODO: Integrate with blockchain contracts to fetch real-time data
	 * - Connect to smart contracts for treasury and hot wallet addresses
	 * - Fetch token price from DEX or price oracle
	 * - Fetch total supply from contract
	 * - Calculate NAV = totalSupply * tokenPrice
	 *
	 * @return Aggregated token reserves response
	 */
	com.tuttime.admin.dto.TokenReservesAggregatedResponse getAggregatedReserves();

	/**
	 * Get reserves for all networks (per-network breakdown)
	 */
	List<TokenReserveResponse> getAllReserves();

	/**
	 * Get reserves for a specific network
	 */
	TokenReserveResponse getReserveByNetwork(String network);

	/**
	 * Update token reserves for a network
	 * (Called by scheduled job or external service)
	 */
	TokenReserveResponse updateReserve(String network,
	                                   java.math.BigDecimal treasuryBalance,
	                                   java.math.BigDecimal hotWalletBalance,
	                                   java.math.BigDecimal tokenPrice,
	                                   java.math.BigDecimal totalSupply,
	                                   java.math.BigDecimal nav);

	/**
	 * Get all active alarms
	 */
	List<ReserveAlarmResponse> getActiveAlarms();

	/**
	 * Get alarms for a specific network
	 */
	List<ReserveAlarmResponse> getAlarmsByNetwork(String network);

	/**
	 * Create a new alarm
	 */
	ReserveAlarmResponse createAlarm(ReserveAlarmRequest request, Long createdBy);

	/**
	 * Update an existing alarm
	 */
	ReserveAlarmResponse updateAlarm(Long alarmId, ReserveAlarmRequest request);

	/**
	 * Delete an alarm
	 */
	void deleteAlarm(Long alarmId);

	/**
	 * Check and trigger alarms based on current reserve values
	 */
	void checkAndTriggerAlarms();
}

