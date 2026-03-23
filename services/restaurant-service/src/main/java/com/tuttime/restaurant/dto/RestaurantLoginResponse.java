package com.tuttime.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin Login Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantLoginResponse {
    private String token;
    private Long restaurantId;
    private String name;
    private String role;
}

