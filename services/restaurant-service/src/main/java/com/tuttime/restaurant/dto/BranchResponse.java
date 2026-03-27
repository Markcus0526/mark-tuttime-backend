package com.tuttime.restaurant.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {

	private UUID branchId;
	private UUID restaurantId;
	private String address;
	private String phone;
	private String timezone;
	private Double latitude;
	private Double longitude;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
}