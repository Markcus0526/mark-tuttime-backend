package com.tuttime.pos.dto.liko.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoOrderDTO {
	private String orderId;
	private String bookingId;
	private String restaurantId;
	private List<IikoOrderItemDTO> items;
	private Double totalAmount;
	private String orderStatus;
}