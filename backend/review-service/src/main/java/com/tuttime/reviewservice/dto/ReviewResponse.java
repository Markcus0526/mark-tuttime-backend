package com.tuttime.reviewservice.dto;

import lombok.Data;

@Data
public class ReviewResponse {
  private Long id;
  private Long userId;
  private Long restaurantId;
  private Double rating;
  private String comment;
}
