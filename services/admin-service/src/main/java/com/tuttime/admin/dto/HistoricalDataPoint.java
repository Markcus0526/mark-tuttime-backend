package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Historical Data Point DTO
 *
 * <p>Represents a single data point in a historical time series.
 * Used for AP and APART growth graphs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDataPoint {
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime timestamp;  // When this data point was recorded
	private BigDecimal value;  // The value at this timestamp (AP Credits or APART tokens)
}

