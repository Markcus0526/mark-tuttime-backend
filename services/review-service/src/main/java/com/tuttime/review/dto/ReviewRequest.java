package com.tuttime.review.dto;

import lombok.Data;

@Data
public class ReviewRequest {
  private Long userId;
  private Long restaurantId;
  private Double rating;
  private String comment;
}
