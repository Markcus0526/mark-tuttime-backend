package com.tuttime.pos.dto.rkeeper.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperOrderDTO {
	private String orderCode;
	private String reservationId;
	private String restaurantCode;
	private List<RKeeperOrderItemDTO> orderItems;
	private Double totalSum;
	private String status;
}
