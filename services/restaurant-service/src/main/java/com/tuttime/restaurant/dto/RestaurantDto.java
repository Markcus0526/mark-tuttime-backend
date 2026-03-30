package com.tuttime.restaurant.dto;

import lombok.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.util.UUID;

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
