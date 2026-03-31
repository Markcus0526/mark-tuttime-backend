package com.tuttime.pos.dto.liko.order;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoModifierDTO {
	private String modifierId;
	private Double priceDelta;
}
