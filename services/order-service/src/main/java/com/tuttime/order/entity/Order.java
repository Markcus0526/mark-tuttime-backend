package com.tuttime.order.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders")
public class Order {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "booking_id")
	private Long bookingId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private OrderStatus status = OrderStatus.CREATED;

	@Column(name = "items", columnDefinition = "jsonb")
	private String itemsJson;

	public static OrderBuilder builder() {
		return new OrderBuilder();
	}

	public static class OrderBuilder {
		private Order order = new Order();

		public OrderBuilder bookingId(Long bookingId) {
			order.bookingId = bookingId;
			return this;
		}

		public OrderBuilder status(OrderStatus status) {
			order.status = status;
			return this;
		}

		public OrderBuilder itemsJson(String itemsJson) {
			order.itemsJson = itemsJson;
			return this;
		}

		public Order build() {
			return order;
		}
	}

	@Column(name = "total_amount")
	private BigDecimal totalAmount;

	@Column(name = "paid_amount")
	private BigDecimal paidAmount;

	@Column(name = "cancel_reason")
	private String cancelReason;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt = LocalDateTime.now();
}
