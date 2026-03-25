package com.tuttime.restaurant.dto;

import lombok.*;
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDto {

	private Long branchId;
	private Long restaurantId;
	private String name;
	private String address;
	private String phone;
	private String timezone;
	private Point location;

}
