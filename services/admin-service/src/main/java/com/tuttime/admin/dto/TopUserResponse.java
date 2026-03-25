package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Top User Response DTO for leaderboards
 *
 * <p>Represents a user in a leaderboard ranking by AP Credits or APART balance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUserResponse {
	private Long userId;
	private String username;
	private String email;
	private String firstName;
	private String lastName;
	private BigDecimal balance; // AP Credits or APART balance depending on leaderboard type
	private Integer rank;
	private String country;
	private String city;
	private Boolean isActive;
	private String statusLevel; // BRONZE, GOLD, PLATINUM, etc.
}

