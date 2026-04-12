package com.tuttime.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {

	private UUID mediaId;
	private String entityType; // RESTAURANT, BRANCH, ZONE, TABLE
	private UUID entityId;
	private String url;
	private String altText;
	private boolean primaryEntity;
	private String mediaType; // IMAGE, VIDEO
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

}