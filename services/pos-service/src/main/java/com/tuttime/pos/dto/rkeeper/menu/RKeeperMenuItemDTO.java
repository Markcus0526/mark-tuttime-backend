package com.tuttime.pos.dto.rkeeper.menu;

import com.tuttime.pos.dto.rkeeper.order.RKeeperModifierDTO;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperMenuItemDTO {
	private String productCode;
	private String productName;
	private Double basePrice;
	private List<RKeeperModifierDTO> modifiers;
}
