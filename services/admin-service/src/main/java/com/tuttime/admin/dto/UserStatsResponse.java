package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * User Stats Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
	private Long totalUsers;
	private Long newUsers;
	private Long activeUsers;
	private Long blockedUsers;
	private List<DailyUserGrowth> dailyGrowth;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DailyUserGrowth {
		private LocalDate date;
		private Long newUsers;
		private Long activeUsers;
	}
}

