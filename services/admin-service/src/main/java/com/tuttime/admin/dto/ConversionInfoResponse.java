package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Conversion Info Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionInfoResponse {
	private Long userId;
	private BigDecimal lastMonthApartConversion;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime nextConversionTime;
	private BigDecimal thisMonthForecastAmount;
}

