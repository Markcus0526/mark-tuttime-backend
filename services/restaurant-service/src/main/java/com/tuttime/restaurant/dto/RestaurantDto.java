package com.tuttime.restaurant.dto;

import lombok.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantDto {

	private Long restaurantId;
	private Long ownerUserId;
	private String name;
	private String description;
	private String address;
	private Point location;
	private String phone;
	private String website;
	private BigDecimal rating;

}
