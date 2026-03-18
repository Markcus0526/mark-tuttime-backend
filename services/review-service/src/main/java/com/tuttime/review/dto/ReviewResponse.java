package com.tuttime.review.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewResponse {
  private Long id;
  private Long userId;
  private Long restaurantId;
  private Double rating;
  private String comment;
}
