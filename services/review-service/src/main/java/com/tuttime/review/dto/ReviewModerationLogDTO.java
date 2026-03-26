package com.tuttime.review.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewModerationLogDTO {

	private UUID id;
	private UUID reviewId;
	private UUID moderatorId;
	private String action;
	private String reason;
	private Instant createdAt;
}