package com.tuttime.order.entity;

import com.tuttime.common.entity.BaseLockingEntity;
import com.tuttime.common.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "order_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE order_service.orders SET deleted_at = now() WHERE id = ?")
public class Order extends BaseLockingEntity {

	@Column(name = "booking_id", nullable = false)
	private UUID bookingId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(columnDefinition = "order_status") // Maps to your PG Enum
	private OrderStatus status = OrderStatus.CREATED;

	@Builder.Default
	@Column(name = "total_amount", nullable = false)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<OrderItem> items = new ArrayList<>();

	// Comment because performance problem
//	@PrePersist
//	@PreUpdate
//	public void preSave() {
//		recalculateTotal();
//	}

	public void addItem(OrderItem item) {
		items.add(item);
		item.setOrder(this);
		calculateTotal();
	}

	public void calculateTotal() {
		this.totalAmount = items.stream()
				.map(OrderItem::calculateSubtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public void recalculateTotal() {
		this.totalAmount = this.items.stream()
				.filter(item -> item.getDeletedAt() == null) // Important if using soft deletes
				.map(OrderItem::getSubtotal) // Uses the logic inside OrderItem
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}