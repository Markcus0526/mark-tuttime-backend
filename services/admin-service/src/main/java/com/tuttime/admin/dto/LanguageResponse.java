package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Language Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageResponse {

	private Long languageId;
	private String languageCode;
	private String languageName;
	private String nativeName;
	private Boolean isActive;
	private Boolean isDefault;
	private Integer displayOrder;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
	private Long createdBy;
}

