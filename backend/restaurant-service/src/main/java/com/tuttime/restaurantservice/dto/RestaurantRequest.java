package com.tuttime.restaurantservice.dto;

import lombok.Data;

@Data
public class RestaurantRequest {
  private String name;
  private String description;
  private String address;
}
