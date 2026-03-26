package com.tuttime.pos.dto.liko.order;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoModifierDTO {
	private String modifierId;
	private Double priceDelta;
}
