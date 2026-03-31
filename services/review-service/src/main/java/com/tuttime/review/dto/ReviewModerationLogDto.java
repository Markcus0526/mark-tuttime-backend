package com.tuttime.review.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewModerationLogDto {

	private UUID id;
	private UUID reviewId;
	private UUID moderatorId;
	private String action;
	private String reason;
	private Instant createdAt;
}