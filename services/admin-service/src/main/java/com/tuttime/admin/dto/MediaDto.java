package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for media information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDto {

	private Long mediaId;
	private String mediaType; // IMAGE, VIDEO
	private String mediaUrl;
	private String thumbnailUrl;
	private Integer width;
	private Integer height;
	private Integer durationSeconds;
	private Integer displayOrder;
}

