package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Status Level Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusLevelResponse {

	private Long levelId;
	private String levelName;
	private BigDecimal minApartBalance;
	private BigDecimal apBoostPerHour;
	private Boolean includesProAutoMining;
	private Integer displayOrder;
	private Boolean isActive;
	private String description;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
	private Long createdBy;
}

