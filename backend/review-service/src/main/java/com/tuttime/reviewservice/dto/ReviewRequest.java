package com.tuttime.reviewservice.dto;

import lombok.Data;

@Data
public class ReviewRequest {
  private Long userId;
  private Long restaurantId;
  private Double rating;
  private String comment;
}
