package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.GeolocationStatsResponse;
import com.tuttime.admin.dto.TopStakingResponse;
import com.tuttime.admin.dto.TopUserResponse;
import com.tuttime.admin.grpc.client.UserServiceGrpcClient;
import com.tuttime.admin.service.AdminLeaderboardService;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Leaderboard Service Implementation
 *
 * <p>Provides leaderboard and geolocation analytics data by aggregating
 * data from domain services via gRPC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLeaderboardServiceImpl implements AdminLeaderboardService {

	private final UserServiceGrpcClient userServiceGrpcClient;
	// TODO: Uncomment when staking-service is deployed
	// private final StakingServiceGrpcClient stakingServiceGrpcClient;

	@Override
	public List<TopUserResponse> getTopUsersByApCredits(Integer limit, String period) {
		try {
			GetTopUsersByApCreditsResponse response = userServiceGrpcClient.getTopUsersByApCredits(limit, period);

			return response.getUsersList().stream()
					.map(this::mapTopUser)
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error getting top users by AP Credits: limit={}, period={}", limit, period, e);
			throw new BusinessException("Failed to get top users by AP Credits: " + e.getMessage());
		}
	}

	@Override
	public List<TopUserResponse> getTopUsersByApartBalance(Integer limit, String period) {
		try {
			GetTopUsersByApartBalanceResponse response = userServiceGrpcClient.getTopUsersByApartBalance(limit, period);

			return response.getUsersList().stream()
					.map(this::mapTopUser)
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error getting top users by APART balance: limit={}, period={}", limit, period, e);
			throw new BusinessException("Failed to get top users by APART balance: " + e.getMessage());
		}
	}

	// TODO: Uncomment when staking-service is deployed
	@Override
	public List<TopStakingResponse> getTopStakings(String sortBy, Long poolId, Integer limit) {
		// try {
		//     GetTopStakingsResponse response = stakingServiceGrpcClient.getTopStakings(sortBy, poolId, limit);
		//
		//     return response.getStakingsList().stream()
		//             .map(this::mapTopStaking)
		//             .collect(Collectors.toList());
		// } catch (Exception e) {
		//     log.error("Error getting top stakings: sortBy={}, poolId={}, limit={}", sortBy, poolId, limit, e);
		//     throw new BusinessException("Failed to get top stakings: " + e.getMessage());
		// }
		// Staking service not deployed - return empty list
		log.warn("Staking service not deployed - returning empty top stakings list");
		return List.of();
	}

	@Override
	public List<GeolocationStatsResponse> getGeolocationStats(String country, String city) {
		try {
			GetGeolocationStatsResponse response = userServiceGrpcClient.getGeolocationStats(country, city);

			return response.getStatsList().stream()
					.map(this::mapGeolocationStat)
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error getting geolocation stats: country={}, city={}", country, city, e);
			throw new BusinessException("Failed to get geolocation stats: " + e.getMessage());
		}
	}

	/**
	 * Map TopUser proto to TopUserResponse DTO
	 */
	private TopUserResponse mapTopUser(TopUser proto) {
		String username = proto.getUsername();
		if (username == null || username.isEmpty()) {
			// Construct username from first and last name
			String firstName = proto.getFirstName() != null ? proto.getFirstName() : "";
			String lastName = proto.getLastName() != null ? proto.getLastName() : "";
			username = (firstName + " " + lastName).trim();
			if (username.isEmpty()) {
				username = proto.getEmail(); // Fallback to email
			}
		}

		return TopUserResponse.builder()
				.userId(proto.getUserId())
				.username(username)
				.email(proto.getEmail())
				.firstName(proto.getFirstName())
				.lastName(proto.getLastName())
				.balance(new BigDecimal(proto.getBalance()))
				.rank(proto.getRank())
				.country(proto.getCountry())
				.city(proto.getCity())
				.isActive(proto.getIsActive())
				.statusLevel(proto.getStatusLevel())
				.build();
	}

	/**
	 * Map TopStaking proto to TopStakingResponse DTO
	 */
	private TopStakingResponse mapTopStaking(TopStaking proto) {
		return TopStakingResponse.builder()
				.stakingId(proto.getStakingId())
				.userId(proto.getUserId())
				.username(proto.getUsername())
				.poolId(proto.getPoolId())
				.poolName(proto.getPoolName())
				.amount(new BigDecimal(proto.getAmount()))
				.apyPercentage(new BigDecimal(proto.getApyPercentage()))
				.miningBonus(new BigDecimal(proto.getMiningBonus()))
				.rewardsEarned(new BigDecimal(proto.getRewardsEarned()))
				.rank(proto.getRank())
				.status(proto.getStatus())
				.build();
	}

	/**
	 * Map GeolocationStat proto to GeolocationStatsResponse DTO
	 */
	private GeolocationStatsResponse mapGeolocationStat(GeolocationStat proto) {
		return GeolocationStatsResponse.builder()
				.country(proto.getCountry())
				.city(proto.getCity())
				.usersCount(proto.getUsersCount())
				.activeMinersCount(proto.getActiveMinersCount())
				.averageApMiningSpeed(new BigDecimal(proto.getAverageApMiningSpeed()))
				.averageApartReserve(new BigDecimal(proto.getAverageApartReserve()))
				.lastConversionDistributedAmount(new BigDecimal(proto.getLastConversionDistributedAmount()))
				.totalStakedAmount(new BigDecimal(proto.getTotalStakedAmount()))
				.build();
	}
}

