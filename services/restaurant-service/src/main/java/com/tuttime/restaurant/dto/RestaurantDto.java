package com.tuttime.restaurant.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.locationtech.jts.geom.Point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantDto {

	private UUID restaurantId;
	private UUID ownerUserId;
	private String name;
	private String description;
	private String address;
	private Point location;
	private String phone;
	private String website;
	private BigDecimal rating;

}
