package com.tuttime.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {

	private UUID branchId;
	private UUID restaurantId;
	private UUID managerUserId;
	private String address;
	private String phone;
	private String timezone;
	private Double longitude;
	private Double latitude;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
}