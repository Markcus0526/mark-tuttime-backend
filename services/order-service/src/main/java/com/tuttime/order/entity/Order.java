package com.tuttime.order.entity;

import com.tuttime.common.entity.BaseLockingEntity;
import com.tuttime.common.entity.BaseUuidAnDeleteEntity;
import com.tuttime.common.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "order_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
public class Order extends BaseUuidAnDeleteEntity {

	@Column(name = "booking_id", nullable = false)
	private UUID bookingId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "branch_id", nullable = false)
	private UUID branchId;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM) // CRITICAL for PG Enum
	@Column(name = "status", nullable = false)
	private OrderStatus status = OrderStatus.CREATED;

	@Builder.Default
	@Column(name = "total_amount", nullable = false)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	private List<OrderItem> items = new ArrayList<>();

	/**
	 * Helper to keep totalAmount in sync
	 */
	public void addItem(OrderItem item) {
		items.add(item);
		item.setOrder(this);
		calculateTotal();
	}

	public void calculateTotal() {
		this.totalAmount = items.stream()
				.map(item -> {
					// Ensure subtotal is calculated in the item before summing
					return item.calculateSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
				})
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
