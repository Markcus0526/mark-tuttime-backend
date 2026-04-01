package com.tuttime.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "modifiers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Modifier extends BaseUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "modifier_group_id", nullable = false)
	private ModifierGroup group;

	@Column(nullable = false)
	private String name;

	private String description;

	@Column(name = "price_adjustment", nullable = false, precision = 10, scale = 2)
	@Builder.Default
	private BigDecimal priceAdjustment = BigDecimal.ZERO;
}