package com.tuttime.admin.util;

import com.tuttime.admin.dto.Period;
import com.tuttime.common.exception.BusinessException;

/**
 * Utility class for converting string period values to Period enum
 */
public class PeriodConverter {

	/**
	 * Convert string period to Period enum (case-insensitive)
	 *
	 * @param periodStr String period value (today, yesterday, week, month, year, total)
	 * @return Period enum
	 * @throws BusinessException if period string is invalid
	 */
	public static Period fromString(String periodStr) {
		if (periodStr == null || periodStr.trim().isEmpty()) {
			return Period.TOTAL; // Default to TOTAL
		}

		String normalized = periodStr.trim().toUpperCase();

		return switch (normalized) {
			case "TODAY" -> Period.TODAY;
			case "YESTERDAY" -> Period.YESTERDAY;
			case "WEEK" -> Period.WEEK;
			case "MONTH" -> Period.MONTH;
			case "YEAR" -> Period.YEAR;
			case "TOTAL" -> Period.TOTAL;
			default -> throw new BusinessException(
					"Invalid period value: " + periodStr + ". Valid values are: today, yesterday, week, month, year, total");
		};
	}
}

