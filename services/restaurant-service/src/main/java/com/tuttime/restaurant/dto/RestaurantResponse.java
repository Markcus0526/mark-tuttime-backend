package com.tuttime.restaurant.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantResponse {

	private UUID restaurantId;
	private UUID ownerUserId;
	private String name;
	private String description;
	private String address;
	private String phone;
	private String website;
	private Double rating;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

}