package com.tuttime.review.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {

	private UUID id;
	private UUID userId;
	private UUID restaurantId;
	private int rating;
	private String comment;
	private String status;
	private Instant createdAt;
	private Instant updatedAt;

}