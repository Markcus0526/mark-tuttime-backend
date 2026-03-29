package com.tuttime.restaurant.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {

	private UUID branchId;
	private UUID restaurantId;
	private String address;
	private String phone;
	private String timezone;
	private Double longitude;
	private Double latitude;
}