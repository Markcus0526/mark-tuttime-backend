package com.tuttime.menu.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModifierDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal priceAdjustment;
}