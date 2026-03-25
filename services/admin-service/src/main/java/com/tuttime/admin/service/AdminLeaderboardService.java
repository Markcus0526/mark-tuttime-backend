package com.tuttime.admin.service;

import com.tuttime.admin.dto.GeolocationStatsResponse;
import com.tuttime.admin.dto.TopStakingResponse;
import com.tuttime.admin.dto.TopUserResponse;

import java.util.List;

/**
 * Admin Leaderboard Service Interface
 *
 * <p>Provides leaderboard and geolocation analytics data for admin panel.
 */
public interface AdminLeaderboardService {

	/**
	 * Get top users by AP Credits balance
	 *
	 * @param limit  Maximum number of users to return (default: 100)
	 * @param period Optional period filter (TODAY, WEEK, MONTH, YEAR, TOTAL)
	 * @return List of top users ranked by AP Credits balance
	 */
	List<TopUserResponse> getTopUsersByApCredits(Integer limit, String period);

	/**
	 * Get top users by APART token balance
	 *
	 * @param limit  Maximum number of users to return (default: 100)
	 * @param period Optional period filter (TODAY, WEEK, MONTH, YEAR, TOTAL)
	 * @return List of top users ranked by APART balance
	 */
	List<TopUserResponse> getTopUsersByApartBalance(Integer limit, String period);

	/**
	 * Get top staking positions
	 *
	 * @param sortBy Sort by "apy" or "miningBonus"
	 * @param poolId Optional pool ID filter
	 * @param limit  Maximum number of positions to return
	 * @return List of top staking positions
	 */
	List<TopStakingResponse> getTopStakings(String sortBy, Long poolId, Integer limit);

	/**
	 * Get geolocation statistics
	 *
	 * @param country Optional country filter
	 * @param city    Optional city filter (requires country)
	 * @return List of geolocation statistics per location
	 */
	List<GeolocationStatsResponse> getGeolocationStats(String country, String city);
}

