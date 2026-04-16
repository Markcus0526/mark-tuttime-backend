package com.tuttime.booking.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_items", schema = "booking_service")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingItem extends BaseUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "booking_id", nullable = false)
	private Booking booking;

	@Column(name = "menu_item_id", nullable = false)
	private UUID menuItemId;

	private Integer quantity;
	private BigDecimal unitPrice;
	private BigDecimal totalPrice;
	private String notes;
}
