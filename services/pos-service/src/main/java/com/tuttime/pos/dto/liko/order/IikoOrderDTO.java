package com.tuttime.pos.dto.liko.order;

import lombok.*;

import java.util.List;

@Getter
@Setter
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