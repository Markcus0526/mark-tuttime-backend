package com.tuttime.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantResponse {

	private UUID restaurantId;
	private UUID ownerUserId;
	private String name;
	private String description;
	private String address;
	private Double longitude;
	private Double latitude;
	private String phone;
	private String website;
	private BigDecimal rating;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
}