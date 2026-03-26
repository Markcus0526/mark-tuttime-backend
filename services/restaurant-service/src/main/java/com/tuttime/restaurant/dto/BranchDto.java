package com.tuttime.restaurant.dto;

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
public class BranchDto {

	private UUID branchId;
	private UUID restaurantId;
	private String name;
	private String address;
	private String phone;
	private String timezone;
	private Point location;

}
