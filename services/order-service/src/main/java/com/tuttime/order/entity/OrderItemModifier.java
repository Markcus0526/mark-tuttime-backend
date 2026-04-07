package com.tuttime.order.entity;

import com.tuttime.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item_modifiers", schema = "order_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE order_service.order_item_modifiers SET deleted_at = now() WHERE id = ?")
public class OrderItemModifier extends BaseSoftDeleteEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_item_id")
	private OrderItem orderItem;

	@Column(name = "modifier_id", nullable = false)
	private UUID modifierId;

	@Column(name = "price_adjustment")
	private BigDecimal priceAdjustment;
}