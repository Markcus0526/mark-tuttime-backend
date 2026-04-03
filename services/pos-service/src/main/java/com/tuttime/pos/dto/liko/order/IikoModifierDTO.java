package com.tuttime.pos.dto.liko.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoModifierDTO {
	private String modifierId;
	private Double priceDelta;
}
