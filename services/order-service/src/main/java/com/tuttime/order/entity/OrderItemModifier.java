package com.tuttime.order.entity;

import com.tuttime.common.entity.BaseLockingEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item_modifiers", schema = "order_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OrderItemModifier extends BaseLockingEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_item_id")
	private OrderItem orderItem;

	@Column(name = "modifier_id", nullable = false)
	private UUID modifierId;

	@Column(name = "price_adjustment")
	private BigDecimal priceAdjustment;
}