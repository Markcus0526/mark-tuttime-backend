package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseDeleteEntity;
import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "modifiers")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Modifier extends BaseDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "modifier_id", nullable = false, updatable = false)
	private UUID modifierId;

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