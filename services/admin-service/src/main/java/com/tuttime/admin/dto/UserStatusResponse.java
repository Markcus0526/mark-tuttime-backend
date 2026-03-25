package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * User Status Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusResponse {
	private Long userId;
	private String statusLevel; // BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, ELITE
	private BigDecimal statusBonusApPerHour;
}

