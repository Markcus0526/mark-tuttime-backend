package com.tuttime.order.entity;

import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "order_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OrderItem extends BaseUuidAnDeleteEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private Order order;

	@Column(name = "menu_item_id", nullable = false)
	private UUID menuItemId;

	private Integer quantity;

	private BigDecimal price;

	@OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<OrderItemModifier> modifiers = new ArrayList<>();

	public BigDecimal calculateSubtotal() {
		BigDecimal modifierTotal = modifiers.stream()
				.map(OrderItemModifier::getPriceAdjustment)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return price.add(modifierTotal).multiply(BigDecimal.valueOf(quantity));
	}

	public BigDecimal getSubtotal() {
		// 1. Start with the base price of the item
		BigDecimal currentPrice = this.price != null ? this.price : BigDecimal.ZERO;

		// 2. Add the sum of all modifier price adjustments
		BigDecimal modifiersTotal = this.modifiers.stream()
				// Only include modifiers that aren't soft-deleted
//				.filter(mod -> mod.getDeletedAt() == null)
				.map(OrderItemModifier::getPriceAdjustment)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 3. (Base + Modifiers) * Quantity
		return currentPrice.add(modifiersTotal)
				.multiply(BigDecimal.valueOf(this.quantity != null ? this.quantity : 0));
	}
}