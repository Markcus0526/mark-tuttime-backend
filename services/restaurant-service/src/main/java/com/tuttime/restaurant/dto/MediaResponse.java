package com.tuttime.restaurant.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {

	private UUID mediaId;
	private String entityType; // RESTAURANT, BRANCH, ZONE, TABLE
	private UUID entityId;
	private String url;
	private String altText;
	private boolean isPrimary;
	private String mediaType; // IMAGE, VIDEO
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

}