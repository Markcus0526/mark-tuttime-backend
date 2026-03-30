package com.tuttime.restaurant.dto;

import lombok.*;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDto {

	private UUID branchId;
	private UUID restaurantId;
	private String name;
	private String address;
	private String phone;
	private String timezone;
	private Point location;

}
