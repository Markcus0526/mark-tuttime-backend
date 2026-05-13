package com.tuttime.order.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "order_tables", schema = "order_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTable {

	@EmbeddedId
	private OrderTableId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("orderId")
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;
}